package com.rcforb.audio

import com.rcforb.util.Log
import kotlinx.coroutines.*
import javax.sound.sampled.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class CodecType { OPUS, SPEEX }

/**
 * macOS desktop audio bridge using javax.sound.sampled (CoreAudio backend).
 * Mirrors the Android AudioBridge byte-for-byte.
 */
class AudioBridge {
    private var codecType = CodecType.OPUS
    private var isActive = false
    private var rxPacketCount = 0

    private var playbackLine: SourceDataLine? = null
    private val sampleRate = 48000

    private var opusDecoder: OpusDecoder? = null
    private var opusEncoder: OpusEncoder? = null
    private var speexDecoder: SpeexDecoder? = null
    private var speexEncoder: SpeexEncoder? = null

    private var isTXActive = false
    private var savedVolume: Float = 1.0f
    private var currentVolume: Float = 1.0f
    private var captureLine: TargetDataLine? = null
    private var txJob: Job? = null
    private val txFrameSize = 960

    private val pendingPcm = mutableListOf<ByteArray>()
    private val batchFrames = 4
    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var batchJob: Job? = null
    private var rxSpeexFrameCount = 2

    var onEncodedAudio: ((ByteArray) -> Unit)? = null

    fun start(codec: CodecType = CodecType.OPUS) {
        if (isActive) return
        codecType = codec
        isActive = true
        rxPacketCount = 0
        pendingPcm.clear()

        if (codec == CodecType.OPUS) {
            opusDecoder = OpusDecoder()
            opusEncoder = OpusEncoder()
            speexDecoder = null
            speexEncoder = null
        } else {
            speexDecoder = SpeexDecoder()
            speexEncoder = SpeexEncoder()
            opusDecoder = null
            opusEncoder = null
        }

        setupPlaybackLine()
    }

    private fun setupPlaybackLine() {
        try {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(format, 8192 * 4)
            line.start()
            playbackLine = line
            applyVolumeOnLine(currentVolume)
        } catch (e: Exception) {
            Log.e("AudioBridge", "Failed to open playback line: ${e.message}")
        }
    }

    private fun applyVolumeOnLine(level: Float) {
        val line = playbackLine ?: return
        try {
            if (line.isControlSupported(FloatControl.Type.VOLUME)) {
                val ctl = line.getControl(FloatControl.Type.VOLUME) as FloatControl
                ctl.value = level.coerceIn(ctl.minimum, ctl.maximum)
                return
            }
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val ctl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val clamped = level.coerceIn(0.0001f, 1f)
                val db = (20.0 * Math.log10(clamped.toDouble())).toFloat()
                ctl.value = db.coerceIn(ctl.minimum, ctl.maximum)
            }
        } catch (_: Exception) {}
    }

    fun pushRXAudio(data: ByteArray) {
        if (!isActive) return

        val pcm = if (codecType == CodecType.OPUS) {
            opusDecoder?.decode(data)
        } else {
            speexDecoder?.decode(data)
        } ?: return

        if (pcm.isEmpty()) return

        rxPacketCount++
        if (rxPacketCount == 1 && codecType == CodecType.SPEEX) {
            rxSpeexFrameCount = (pcm.size / 320).coerceAtLeast(1)
        }

        val outputPcm = if (codecType == CodecType.SPEEX) upsample8to48(pcm) else pcm

        synchronized(pendingPcm) {
            pendingPcm.add(outputPcm)
            if (pendingPcm.size >= batchFrames) {
                flushToPlayer()
            } else if (batchJob == null) {
                batchJob = audioScope.launch {
                    delay(40)
                    synchronized(pendingPcm) { flushToPlayer() }
                }
            }
        }
    }

    private fun flushToPlayer() {
        batchJob?.cancel()
        batchJob = null
        if (pendingPcm.isEmpty()) return
        var totalSize = 0
        for (chunk in pendingPcm) totalSize += chunk.size
        val merged = ByteArray(totalSize)
        var offset = 0
        for (chunk in pendingPcm) {
            System.arraycopy(chunk, 0, merged, offset, chunk.size)
            offset += chunk.size
        }
        pendingPcm.clear()
        try { playbackLine?.write(merged, 0, merged.size) } catch (_: Exception) {}
    }

    fun startTX() {
        if (!isActive || isTXActive) return
        isTXActive = true

        savedVolume = currentVolume
        applyVolumeOnLine(0.05f)

        val txSampleRate = if (codecType == CodecType.SPEEX) 8000 else sampleRate
        val txFrame = if (codecType == CodecType.SPEEX) 160 * rxSpeexFrameCount else txFrameSize

        try {
            try { captureLine?.close() } catch (_: Exception) {}
            val format = AudioFormat(txSampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format, txFrame * 2 * 4)
            line.start()
            captureLine = line

            txJob = audioScope.launch {
                val buffer = ByteArray(txFrame * 2)
                while (isTXActive) {
                    val read = try { line.read(buffer, 0, buffer.size) } catch (_: Exception) { -1 }
                    if (read == buffer.size) {
                        val encoded = if (codecType == CodecType.OPUS) {
                            opusEncoder?.encode(buffer)
                        } else {
                            speexEncoder?.encode(buffer)
                        }
                        if (encoded != null) {
                            onEncodedAudio?.invoke(encoded)
                        }
                    } else if (read < 0) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioBridge", "Failed to start TX: ${e.message}")
            isTXActive = false
            applyVolumeOnLine(savedVolume)
        }
    }

    fun stopTX() {
        if (!isTXActive) return
        isTXActive = false
        txJob?.cancel()
        txJob = null
        try {
            captureLine?.stop()
            captureLine?.close()
        } catch (_: Exception) {}
        captureLine = null

        synchronized(pendingPcm) {
            try {
                playbackLine?.stop()
                playbackLine?.close()
            } catch (_: Exception) {}
            playbackLine = null
            currentVolume = savedVolume
            setupPlaybackLine()
        }
    }

    fun setVolume(level: Float) {
        currentVolume = level
        applyVolumeOnLine(level)
    }

    fun stop() {
        stopTX()
        isActive = false
        batchJob?.cancel()
        batchJob = null
        try {
            playbackLine?.stop()
            playbackLine?.close()
        } catch (_: Exception) {}
        playbackLine = null
        opusDecoder?.release(); opusDecoder = null
        opusEncoder?.release(); opusEncoder = null
        speexDecoder?.release(); speexDecoder = null
        speexEncoder?.release(); speexEncoder = null
        onEncodedAudio = null
        rxPacketCount = 0
        pendingPcm.clear()
    }

    fun micTest(onComplete: (Boolean) -> Unit) {
        audioScope.launch {
            try {
                val testRate = 8000
                val testFrame = 160
                val testSeconds = 2
                val totalSamples = testRate * testSeconds
                val recorded = mutableListOf<ByteArray>()

                applyVolumeOnLine(0f)

                val format = AudioFormat(testRate.toFloat(), 16, 1, true, false)
                val info = DataLine.Info(TargetDataLine::class.java, format)
                val rec = AudioSystem.getLine(info) as TargetDataLine
                rec.open(format, testFrame * 2 * 4)
                rec.start()
                var samplesRead = 0
                while (samplesRead < totalSamples) {
                    val buf = ByteArray(testFrame * 2)
                    val read = rec.read(buf, 0, buf.size)
                    if (read > 0) {
                        recorded.add(buf.copyOf(read))
                        samplesRead += read / 2
                    }
                }
                rec.stop()
                rec.close()

                var maxSample: Short = 0
                for (chunk in recorded) {
                    val bb = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
                    while (bb.remaining() >= 2) {
                        val s = kotlin.math.abs(bb.short.toInt()).toShort()
                        if (s > maxSample) maxSample = s
                    }
                }

                val encoder = SpeexEncoder()
                val decoder = SpeexDecoder()
                val decoded = mutableListOf<ByteArray>()
                for (chunk in recorded) {
                    if (chunk.size == testFrame * 2) {
                        val enc = encoder.encode(chunk)
                        if (enc != null) {
                            val dec = decoder.decode(enc)
                            if (dec != null) decoded.add(dec)
                        }
                    }
                }
                encoder.release()
                decoder.release()

                val playFmt = AudioFormat(8000f, 16, 1, true, false)
                val playInfo = DataLine.Info(SourceDataLine::class.java, playFmt)
                val player = AudioSystem.getLine(playInfo) as SourceDataLine
                player.open(playFmt, 8192)
                player.start()
                for (chunk in decoded) {
                    player.write(chunk, 0, chunk.size)
                }
                Thread.sleep(2500)
                player.stop()
                player.close()

                applyVolumeOnLine(currentVolume)
                onComplete(maxSample > 100)
            } catch (e: Exception) {
                Log.e("AudioBridge", "Mic test failed: ${e.message}")
                onComplete(false)
            }
        }
    }

    companion object {
        fun upsample8to48(input: ByteArray): ByteArray {
            val srcSamples = input.size / 2
            val dstSamples = srcSamples * 6
            val output = ByteArray(dstSamples * 2)

            val srcBuf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
            val dstBuf = ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN)

            val src = ShortArray(srcSamples) { srcBuf.short }

            for (i in 0 until srcSamples - 1) {
                val s0 = src[i].toFloat()
                val s1 = src[i + 1].toFloat()
                for (j in 0 until 6) {
                    val t = j / 6.0f
                    val interpolated = (s0 + (s1 - s0) * t).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    dstBuf.putShort((i * 6 + j) * 2, interpolated)
                }
            }
            val last = src[srcSamples - 1]
            for (j in 0 until 6) {
                dstBuf.putShort(((srcSamples - 1) * 6 + j) * 2, last)
            }
            return output
        }
    }
}

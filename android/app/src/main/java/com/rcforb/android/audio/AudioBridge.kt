package com.rcforb.android.audio

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class CodecType { OPUS, SPEEX }

class AudioBridge {
    private var codecType = CodecType.OPUS
    private var isActive = false
    private var rxPacketCount = 0

    private var audioTrack: AudioTrack? = null
    private val sampleRate = 48000

    private var opusDecoder: OpusDecoder? = null
    private var opusEncoder: OpusEncoder? = null
    private var speexDecoder: SpeexDecoder? = null
    private var speexEncoder: SpeexEncoder? = null

    // TX state / volume ducking
    private var isTXActive = false
    private var savedVolume: Float = 1.0f
    private var currentVolume: Float = 1.0f
    private var audioRecord: AudioRecord? = null
    private var txJob: Job? = null
    private val txFrameSize = 960 // 20ms at 48kHz

    private val pendingPcm = mutableListOf<ByteArray>()
    private val batchFrames = 4
    private val audioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var batchJob: Job? = null
    private var rxSpeexFrameCount = 2 // frames per RX packet, detected dynamically

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

        setupAudioTrack()
        Log.i("AudioBridge", "Started with $codec codec")
    }

    private fun setupAudioTrack() {
        val bufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
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
            // Detect frames per packet from first RX: pcm bytes / (160 samples * 2 bytes)
            rxSpeexFrameCount = (pcm.size / 320).coerceAtLeast(1)
            Log.i("AudioBridge", "RX packet #1: ${data.size} encoded -> ${pcm.size} PCM bytes = $rxSpeexFrameCount frames/packet")
        } else if (rxPacketCount <= 3) {
            Log.i("AudioBridge", "RX packet #$rxPacketCount: ${data.size} encoded -> ${pcm.size} PCM bytes")
        }

        val outputPcm = if (codecType == CodecType.SPEEX) {
            upsample8to48(pcm)
        } else {
            pcm
        }

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

        audioTrack?.write(merged, 0, merged.size)
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun startTX() {
        if (!isActive || isTXActive) return
        isTXActive = true
        Log.i("AudioBridge", "startTX: ducking volume from $currentVolume to 0.05")

        // Volume ducking — save current volume and reduce to near-silent
        savedVolume = currentVolume
        audioTrack?.setVolume(0.05f)

        // Speex uses 8kHz mic, Opus uses 48kHz
        // Match TX frame count to server's RX frame count for compatibility
        val txSampleRate = if (codecType == CodecType.SPEEX) 8000 else sampleRate
        val txFrame = if (codecType == CodecType.SPEEX) 160 * rxSpeexFrameCount else txFrameSize
        Log.i("AudioBridge", "TX config: rate=$txSampleRate, samplesPerPacket=$txFrame ($rxSpeexFrameCount frames)")

        try {
            try { audioRecord?.release() } catch (_: Exception) {}
            val bufSize = AudioRecord.getMinBufferSize(
                txSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                txSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize.coerceAtLeast(txFrame * 2 * 4)
            )
            Log.i("AudioBridge", "AudioRecord created: state=${audioRecord?.state}, rate=$txSampleRate, frame=$txFrame")
            audioRecord?.startRecording()

            txJob = audioScope.launch {
                val buffer = ByteArray(txFrame * 2) // Int16 = 2 bytes per sample
                var readCount = 0
                while (isTXActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    readCount++
                    if (readCount <= 3) {
                        Log.i("AudioBridge", "TX read #$readCount: $read bytes (expected ${buffer.size}), recordState=${audioRecord?.recordingState}")
                    }
                    if (read == buffer.size) {
                        val encoded = if (codecType == CodecType.OPUS) {
                            opusEncoder?.encode(buffer)
                        } else {
                            speexEncoder?.encode(buffer)
                        }
                        if (encoded != null) {
                            if (readCount <= 3) {
                                Log.i("AudioBridge", "TX frame #$readCount: ${encoded.size} bytes, first10=[${encoded.take(10).joinToString(",") { (it.toInt() and 0xFF).toString() }}]")
                            }
                            onEncodedAudio?.invoke(encoded)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioBridge", "Failed to start TX: ${e.message}")
            isTXActive = false
            audioTrack?.setVolume(savedVolume)
        }
    }

    fun stopTX() {
        if (!isTXActive) return
        isTXActive = false
        txJob?.cancel()
        txJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        // Rebuild AudioTrack to guarantee clean playback state
        Log.i("AudioBridge", "stopTX: rebuilding AudioTrack, restoring volume to $savedVolume")
        synchronized(pendingPcm) {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
            audioTrack = null
            setupAudioTrack()
            audioTrack?.setVolume(savedVolume)
        }
    }

    fun setVolume(level: Float) {
        currentVolume = level
        audioTrack?.setVolume(level)
    }

    fun stop() {
        Log.i("AudioBridge", "Stopped. Total RX packets decoded: $rxPacketCount")
        stopTX()
        isActive = false
        batchJob?.cancel()
        batchJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        opusDecoder = null
        opusEncoder?.release()
        opusEncoder = null
        speexDecoder = null
        speexEncoder?.release()
        speexEncoder = null
        onEncodedAudio = null
        rxPacketCount = 0
        pendingPcm.clear()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun micTest(onComplete: (Boolean) -> Unit) {
        audioScope.launch {
            try {
                val testRate = 8000
                val testFrame = 160
                val testSeconds = 2
                val totalSamples = testRate * testSeconds
                val recorded = mutableListOf<ByteArray>()

                // Mute RX during test
                audioTrack?.setVolume(0f)

                // Record
                val bufSize = AudioRecord.getMinBufferSize(testRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val rec = AudioRecord(MediaRecorder.AudioSource.MIC, testRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize.coerceAtLeast(testFrame * 2 * 4))
                rec.startRecording()
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
                rec.release()

                // Check if we got actual audio (not silence)
                var maxSample: Short = 0
                for (chunk in recorded) {
                    val bb = java.nio.ByteBuffer.wrap(chunk).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    while (bb.remaining() >= 2) {
                        val s = kotlin.math.abs(bb.short.toInt()).toShort()
                        if (s > maxSample) maxSample = s
                    }
                }
                Log.i("AudioBridge", "Mic test: recorded ${recorded.size} frames, maxSample=$maxSample")

                // Encode and decode round-trip
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
                Log.i("AudioBridge", "Mic test: encoded/decoded ${decoded.size} frames")

                // Play back through AudioTrack at 8kHz (no upsample needed)
                val playBuf = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val player = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(8000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(playBuf * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                player.play()
                for (chunk in decoded) {
                    player.write(chunk, 0, chunk.size)
                }
                Thread.sleep(2500) // let buffer drain fully
                player.stop()
                player.release()

                // Restore RX volume
                audioTrack?.setVolume(currentVolume)

                Log.i("AudioBridge", "Mic test complete: maxSample=$maxSample, success=${maxSample > 100}")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(maxSample > 100)
                }
            } catch (e: Exception) {
                Log.e("AudioBridge", "Mic test failed: ${e.message}")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false)
                }
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

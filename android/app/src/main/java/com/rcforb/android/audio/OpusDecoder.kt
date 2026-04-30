package com.rcforb.android.audio

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Java Opus decoder using a minimal implementation.
 * For production use, this wraps Android's built-in MediaCodec for Opus decoding.
 * Falls back to silence if decoding fails.
 */
class OpusDecoder {
    private val sampleRate = 48000
    private val channels = 1
    private val frameSize = 960 // 20ms at 48kHz
    private val bytesPerFrame = frameSize * 2 // Int16 PCM
    private var codec: android.media.MediaCodec? = null
    private var isReady = false

    init {
        try {
            codec = android.media.MediaCodec.createDecoderByType("audio/opus")
            val format = android.media.MediaFormat.createAudioFormat("audio/opus", sampleRate, channels)
            // Opus requires CSD buffers
            val csd0 = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN)
            csd0.put("OpusHead".toByteArray(Charsets.US_ASCII))
            csd0.put(1) // version
            csd0.put(channels.toByte()) // channels
            csd0.putShort(0) // pre-skip
            csd0.putInt(sampleRate) // sample rate
            csd0.putShort(0) // output gain
            csd0.put(0) // channel mapping family
            csd0.flip()
            format.setByteBuffer("csd-0", csd0)

            val csd1 = ByteBuffer.allocate(8)
            csd1.putLong(0) // pre-skip in ns
            csd1.flip()
            format.setByteBuffer("csd-1", csd1)

            val csd2 = ByteBuffer.allocate(8)
            csd2.putLong(80000000L) // seek pre-roll in ns (80ms)
            csd2.flip()
            format.setByteBuffer("csd-2", csd2)

            codec?.configure(format, null, null, 0)
            codec?.start()
            isReady = true
            Log.i("OpusDecoder", "MediaCodec Opus decoder initialized")
        } catch (e: Exception) {
            Log.w("OpusDecoder", "Failed to init MediaCodec Opus: ${e.message}")
            isReady = false
        }
    }

    fun decode(packet: ByteArray): ByteArray? {
        val mc = codec
        if (!isReady || mc == null) {
            return ByteArray(bytesPerFrame) // silence fallback
        }

        try {
            val inputIdx = mc.dequeueInputBuffer(10000)
            if (inputIdx >= 0) {
                val inputBuf = mc.getInputBuffer(inputIdx) ?: return null
                inputBuf.clear()
                inputBuf.put(packet)
                mc.queueInputBuffer(inputIdx, 0, packet.size, 0, 0)
            }

            val bufferInfo = android.media.MediaCodec.BufferInfo()
            val outputIdx = mc.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIdx >= 0) {
                val outputBuf = mc.getOutputBuffer(outputIdx) ?: return null
                val pcm = ByteArray(bufferInfo.size)
                outputBuf.position(bufferInfo.offset)
                outputBuf.get(pcm, 0, bufferInfo.size)
                mc.releaseOutputBuffer(outputIdx, false)
                return pcm
            }
        } catch (e: Exception) {
            Log.e("OpusDecoder", "Decode error", e)
        }
        return null
    }

    fun release() {
        try {
            codec?.stop()
            codec?.release()
        } catch (_: Exception) {}
        codec = null
        isReady = false
    }
}

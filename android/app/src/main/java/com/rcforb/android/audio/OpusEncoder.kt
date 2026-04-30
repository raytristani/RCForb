package com.rcforb.android.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

/**
 * Opus encoder using Android's MediaCodec.
 * Encodes 48kHz mono Int16 PCM into Opus frames for TX audio.
 */
class OpusEncoder {
    private val sampleRate = 48000
    private val channels = 1
    private val bitRate = 24000
    private val frameSize = 960 // 20ms at 48kHz
    private var codec: MediaCodec? = null
    private var isReady = false

    init {
        try {
            codec = MediaCodec.createEncoderByType("audio/opus")
            val format = MediaFormat.createAudioFormat("audio/opus", sampleRate, channels)
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, frameSize * 2)
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec?.start()
            isReady = true
            Log.i("OpusEncoder", "MediaCodec Opus encoder initialized")
        } catch (e: Exception) {
            Log.w("OpusEncoder", "Failed to init MediaCodec Opus encoder: ${e.message}")
            isReady = false
        }
    }

    fun encode(pcm: ByteArray): ByteArray? {
        val mc = codec
        if (!isReady || mc == null) return null

        try {
            val inputIdx = mc.dequeueInputBuffer(10000)
            if (inputIdx >= 0) {
                val inputBuf = mc.getInputBuffer(inputIdx) ?: return null
                inputBuf.clear()
                inputBuf.put(pcm)
                mc.queueInputBuffer(inputIdx, 0, pcm.size, 0, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val outputIdx = mc.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIdx >= 0) {
                val outputBuf = mc.getOutputBuffer(outputIdx) ?: return null
                val encoded = ByteArray(bufferInfo.size)
                outputBuf.position(bufferInfo.offset)
                outputBuf.get(encoded, 0, bufferInfo.size)
                mc.releaseOutputBuffer(outputIdx, false)
                return encoded
            }
        } catch (e: Exception) {
            Log.e("OpusEncoder", "Encode error", e)
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

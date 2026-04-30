package com.rcforb.audio

import com.rcforb.util.Log
import com.sun.jna.Pointer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Speex narrowband encoder via JNA bindings to libspeex_jni.
 * Expects 8 kHz mono Int16 LE PCM input. Quality 8 (matches Android client).
 */
class SpeexEncoder {
    private var handle: Pointer? = null

    init {
        try {
            handle = SpeexNative.lib().rcforb_speex_create_encoder(8)
        } catch (e: Exception) {
            Log.e("SpeexEncoder", "Failed to init Speex encoder: ${e.message}")
            handle = null
        }
    }

    fun encode(pcm8k: ByteArray): ByteArray? {
        val h = handle ?: return null
        val numSamples = pcm8k.size / 2
        if (numSamples == 0) return null
        val pcmShorts = ShortArray(numSamples)
        val bb = ByteBuffer.wrap(pcm8k).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples) pcmShorts[i] = bb.short

        val outBuf = ByteArray(4096)
        val written = SpeexNative.lib().rcforb_speex_encode(h, pcmShorts, numSamples, outBuf, outBuf.size)
        if (written <= 0) return null
        return outBuf.copyOf(written)
    }

    fun release() {
        handle?.let { SpeexNative.lib().rcforb_speex_destroy_encoder(it) }
        handle = null
    }
}

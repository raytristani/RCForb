package com.rcforb.audio

import com.rcforb.util.Log
import com.sun.jna.Pointer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Speex narrowband decoder via JNA bindings to libspeex_jni.
 * 8 kHz narrowband, 160 samples/frame (20 ms).
 */
class SpeexDecoder {
    private var handle: Pointer? = null

    init {
        try {
            handle = SpeexNative.lib().rcforb_speex_create_decoder()
        } catch (e: Exception) {
            Log.e("SpeexDecoder", "Failed to init native Speex: ${e.message}")
            handle = null
        }
    }

    fun decode(packet: ByteArray): ByteArray? {
        val h = handle ?: return ByteArray(320)
        val outBuf = ShortArray(160 * 10)
        val total = SpeexNative.lib().rcforb_speex_decode(h, packet, packet.size, outBuf, outBuf.size)
        if (total <= 0) return null
        val bytes = ByteBuffer.allocate(total * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until total) bytes.putShort(outBuf[i])
        return bytes.array()
    }

    fun release() {
        handle?.let { SpeexNative.lib().rcforb_speex_destroy_decoder(it) }
        handle = null
    }
}

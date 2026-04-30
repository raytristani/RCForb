package com.rcforb.android.audio

import android.util.Log

/**
 * Speex narrowband decoder using native libspeex via JNI.
 * Parameters: 8kHz narrowband, 160 samples/frame (20ms).
 */
class SpeexDecoder {
    private var handle: Long = 0L

    init {
        try {
            handle = SpeexNative.nativeCreate()
            Log.i("SpeexDecoder", "Native Speex decoder initialized (handle=$handle)")
        } catch (e: Exception) {
            Log.e("SpeexDecoder", "Failed to init native Speex: ${e.message}")
            handle = 0L
        }
    }

    fun decode(packet: ByteArray): ByteArray? {
        if (handle == 0L) return ByteArray(320) // silence fallback
        return SpeexNative.nativeDecode(handle, packet)
    }

    fun release() {
        if (handle != 0L) {
            SpeexNative.nativeDestroy(handle)
            handle = 0L
        }
    }
}

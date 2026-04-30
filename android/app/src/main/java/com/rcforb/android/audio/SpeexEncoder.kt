package com.rcforb.android.audio

import android.util.Log

/**
 * Speex narrowband encoder using native libspeex via JNI.
 * Expects 8kHz mono 16-bit PCM input (160 samples per frame).
 */
class SpeexEncoder {
    private var handle: Long = 0L

    init {
        try {
            handle = SpeexNative.nativeCreateEncoder(8) // quality 8, matching C# client
            Log.i("SpeexEncoder", "Native Speex encoder initialized (handle=$handle)")
        } catch (e: Exception) {
            Log.e("SpeexEncoder", "Failed to init native Speex encoder: ${e.message}")
            handle = 0L
        }
    }

    fun encode(pcm8k: ByteArray): ByteArray? {
        if (handle == 0L) return null
        return SpeexNative.nativeEncode(handle, pcm8k)
    }

    fun release() {
        if (handle != 0L) {
            SpeexNative.nativeDestroyEncoder(handle)
            handle = 0L
        }
    }
}

package com.rcforb.android.audio

object SpeexNative {
    init {
        System.loadLibrary("speex_jni")
    }

    external fun nativeCreate(): Long
    external fun nativeDecode(handle: Long, encoded: ByteArray): ByteArray?
    external fun nativeDestroy(handle: Long)

    external fun nativeCreateEncoder(quality: Int): Long
    external fun nativeEncode(handle: Long, pcm: ByteArray): ByteArray?
    external fun nativeDestroyEncoder(handle: Long)
}

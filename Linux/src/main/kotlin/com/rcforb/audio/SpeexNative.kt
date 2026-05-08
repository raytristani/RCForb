package com.rcforb.audio

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

/**
 * Loads libspeex_jni.dylib (which we ship inside the app jar under
 * /native/libspeex_jni.dylib) and exposes a JNA-friendly C surface.
 */
object SpeexNative {

    interface Lib : Library {
        fun rcforb_speex_create_decoder(): Pointer?
        fun rcforb_speex_decode(handle: Pointer, input: ByteArray, inputLen: Int, output: ShortArray, outputMax: Int): Int
        fun rcforb_speex_destroy_decoder(handle: Pointer)
        fun rcforb_speex_create_encoder(quality: Int): Pointer?
        fun rcforb_speex_encode(handle: Pointer, pcm: ShortArray, numSamples: Int, output: ByteArray, outputMax: Int): Int
        fun rcforb_speex_get_frame_size(handle: Pointer): Int
        fun rcforb_speex_destroy_encoder(handle: Pointer)
    }

    @Volatile private var libRef: Lib? = null

    fun lib(): Lib = libRef ?: synchronized(this) {
        libRef ?: load().also { libRef = it }
    }

    private fun load(): Lib {
        val resourceName = "/native/libspeex_jni.dylib"
        val stream = SpeexNative::class.java.getResourceAsStream(resourceName)
            ?: error("Speex native lib not found at $resourceName — did the build task run?")
        val tmp = File.createTempFile("libspeex_jni", ".dylib")
        tmp.deleteOnExit()
        FileOutputStream(tmp).use { out -> stream.copyTo(out) }
        return Native.load(tmp.absolutePath, Lib::class.java)
    }
}

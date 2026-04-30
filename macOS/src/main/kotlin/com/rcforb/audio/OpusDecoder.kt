package com.rcforb.audio

/**
 * Opus decoding is not yet wired on macOS — V10 stations fall back to silence.
 * V7 (Speex) covers the majority of public stations. To enable V10, bundle
 * libopus.dylib and bind it via JNA here (see PORTING.md §9.4).
 */
class OpusDecoder {
    private val frameBytes = 960 * 2
    fun decode(packet: ByteArray): ByteArray? = ByteArray(frameBytes)
    fun release() {}
}

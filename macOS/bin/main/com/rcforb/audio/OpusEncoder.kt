package com.rcforb.audio

/** Opus encoding is not yet wired on macOS — see OpusDecoder.kt for context. */
class OpusEncoder {
    fun encode(pcm: ByteArray): ByteArray? = null
    fun release() {}
}

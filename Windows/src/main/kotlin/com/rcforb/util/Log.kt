package com.rcforb.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Drop-in replacement for android.util.Log with the same surface. */
object Log {
    private val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private fun emit(level: String, tag: String, msg: String, tr: Throwable? = null) {
        println("${ts.format(Date())} $level/$tag: $msg")
        tr?.printStackTrace(System.out)
    }
    fun v(tag: String, msg: String) = emit("V", tag, msg)
    fun d(tag: String, msg: String) = emit("D", tag, msg)
    fun i(tag: String, msg: String) = emit("I", tag, msg)
    fun w(tag: String, msg: String, tr: Throwable? = null) = emit("W", tag, msg, tr)
    fun e(tag: String, msg: String, tr: Throwable? = null) = emit("E", tag, msg, tr)
}

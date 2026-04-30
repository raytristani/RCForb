package com.rcforb.android.protocol

import java.net.URLEncoder
import java.security.MessageDigest

fun md5(input: String): String {
    val digest = MessageDigest.getInstance("MD5")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

fun doubleMD5(password: String): String = md5(md5(password))

fun validationToken(user: String, doubleMD5Pass: String): String {
    val encodedUser = URLEncoder.encode(user, "UTF-8")
    return md5(encodedUser + doubleMD5Pass)
}

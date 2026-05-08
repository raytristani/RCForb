package com.rcforb.services

import com.rcforb.models.AuthResult
import com.rcforb.protocol.md5
import com.rcforb.protocol.validationToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AuthService {
    private const val LOGIN_URL = "https://api.remotehams.com/v2/login.php"

    suspend fun authenticate(user: String, password: String): AuthResult {
        val passMD5 = md5(password)
        return authenticateWithMD5(user, passMD5)
    }

    suspend fun authenticateWithMD5(user: String, passwordMD5: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val passDoubleHash = md5(passwordMD5)
            val encodedUser = URLEncoder.encode(user, "UTF-8")
            val valid = validationToken(user, passDoubleHash)

            val form = "user=$encodedUser&pass=$passDoubleHash&valid=$valid&getkey=true"

            val text = postForm(LOGIN_URL, form)
            if (text.startsWith("Valid")) {
                val parts = text.split(",")
                val apiKey = if (parts.size > 1) parts[1].trim() else null
                AuthResult(success = true, message = text, apiKey = apiKey)
            } else {
                AuthResult(success = false, message = text)
            }
        } catch (e: Exception) {
            AuthResult(success = false, message = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun trackOnline(user: String, passwordMD5: String, orbId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val passDoubleHash = md5(passwordMD5)
            val encodedUser = URLEncoder.encode(user, "UTF-8")
            val valid = md5(encodedUser + passDoubleHash)
            val form = buildString {
                append("user=$encodedUser&pass=$passDoubleHash&varMe=valYou&logonline=true&valid=$valid")
                if (orbId != null) append("&orbid=").append(URLEncoder.encode(orbId, "UTF-8"))
            }
            val text = postForm(LOGIN_URL, form)
            text.contains("Valid")
        } catch (_: Exception) {
            false
        }
    }

    private fun postForm(url: String, formBody: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Content-Length", formBody.toByteArray().size.toString())
        }
        conn.outputStream.use { it.write(formBody.toByteArray(Charsets.UTF_8)) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream?.bufferedReader()?.use { it.readText() } ?: ""
    }
}

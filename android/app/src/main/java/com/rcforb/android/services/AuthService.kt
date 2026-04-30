package com.rcforb.android.services

import com.rcforb.android.models.AuthResult
import com.rcforb.android.protocol.md5
import com.rcforb.android.protocol.validationToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

object AuthService {
    private const val LOGIN_URL = "https://api.remotehams.com/v2/login.php"
    private val client = OkHttpClient()

    suspend fun authenticate(user: String, password: String): AuthResult {
        val passMD5 = md5(password)
        return authenticateWithMD5(user, passMD5)
    }

    suspend fun authenticateWithMD5(user: String, passwordMD5: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val passDoubleHash = md5(passwordMD5)
            val encodedUser = URLEncoder.encode(user, "UTF-8")
            val valid = validationToken(user, passDoubleHash)

            val body = FormBody.Builder()
                .addEncoded("user", encodedUser)
                .addEncoded("pass", passDoubleHash)
                .addEncoded("valid", valid)
                .addEncoded("getkey", "true")
                .build()

            val request = Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: ""

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

            val bodyBuilder = FormBody.Builder()
                .addEncoded("user", encodedUser)
                .addEncoded("pass", passDoubleHash)
                .addEncoded("varMe", "valYou")
                .addEncoded("logonline", "true")
                .addEncoded("valid", valid)
            if (orbId != null) bodyBuilder.addEncoded("orbid", orbId)

            val request = Request.Builder()
                .url(LOGIN_URL)
                .post(bodyBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: ""
            text.contains("Valid")
        } catch (_: Exception) {
            false
        }
    }
}

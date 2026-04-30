package com.rcforb.android.services

import android.content.Context
import com.rcforb.android.models.SavedCredentials
import org.json.JSONObject

object CredentialStore {
    private const val PREFS_NAME = "rcforb_credentials"
    private const val KEY_DATA = "data"
    private const val XOR_KEY: Byte = 0x5A

    fun save(context: Context, user: String, password: String) {
        val json = JSONObject().apply {
            put("user", user)
            put("password", password)
        }
        val data = json.toString().toByteArray(Charsets.UTF_8)
        val encoded = data.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DATA, android.util.Base64.encodeToString(encoded, android.util.Base64.NO_WRAP)).apply()
    }

    fun load(context: Context): SavedCredentials? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded64 = prefs.getString(KEY_DATA, null) ?: return null
        return try {
            val encoded = android.util.Base64.decode(encoded64, android.util.Base64.NO_WRAP)
            val data = encoded.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
            val json = JSONObject(String(data, Charsets.UTF_8))
            SavedCredentials(
                user = json.getString("user"),
                password = json.getString("password")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

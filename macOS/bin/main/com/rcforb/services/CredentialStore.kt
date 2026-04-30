package com.rcforb.services

import com.rcforb.models.SavedCredentials
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.prefs.Preferences

object CredentialStore {
    private const val KEY_DATA = "data"
    private const val XOR_KEY: Byte = 0x5A
    private val prefs: Preferences = Preferences.userRoot().node("com/rcforb/macos/credentials")

    @Serializable
    private data class Stored(val user: String, val password: String)

    fun save(user: String, password: String) {
        val json = Json.encodeToString(Stored.serializer(), Stored(user, password))
        val data = json.toByteArray(Charsets.UTF_8)
        val encoded = data.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
        prefs.put(KEY_DATA, Base64.getEncoder().encodeToString(encoded))
    }

    fun load(): SavedCredentials? {
        val encoded64 = prefs.get(KEY_DATA, null) ?: return null
        return try {
            val encoded = Base64.getDecoder().decode(encoded64)
            val data = encoded.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
            val s = Json.decodeFromString(Stored.serializer(), String(data, Charsets.UTF_8))
            SavedCredentials(s.user, s.password)
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.remove(KEY_DATA)
    }
}

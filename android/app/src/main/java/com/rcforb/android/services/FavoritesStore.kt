package com.rcforb.android.services

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteStation(
    val serverId: String,
    val serverName: String,
    val radioModel: String,
    val description: String,
    val host: String,
    val port: Int,
    val voipPort: Int,
    val isV7: Boolean
)

object FavoritesStore {
    private const val PREFS_NAME = "rcforb_favorites"
    private const val KEY_FAVORITES = "favorites"

    fun save(context: Context, favorites: List<FavoriteStation>) {
        val arr = JSONArray()
        for (f in favorites) {
            arr.put(JSONObject().apply {
                put("serverId", f.serverId)
                put("serverName", f.serverName)
                put("radioModel", f.radioModel)
                put("description", f.description)
                put("host", f.host)
                put("port", f.port)
                put("voipPort", f.voipPort)
                put("isV7", f.isV7)
            })
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }

    fun load(context: Context): List<FavoriteStation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                FavoriteStation(
                    serverId = obj.getString("serverId"),
                    serverName = obj.getString("serverName"),
                    radioModel = obj.optString("radioModel", ""),
                    description = obj.optString("description", ""),
                    host = obj.optString("host", ""),
                    port = obj.optInt("port", 4525),
                    voipPort = obj.optInt("voipPort", 4524),
                    isV7 = obj.optBoolean("isV7", true)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addFavorite(context: Context, station: FavoriteStation) {
        val list = load(context).toMutableList()
        if (list.none { it.serverId == station.serverId }) {
            list.add(station)
            save(context, list)
        }
    }

    fun removeFavorite(context: Context, serverId: String) {
        val list = load(context).toMutableList()
        list.removeAll { it.serverId == serverId }
        save(context, list)
    }

    fun isFavorite(context: Context, serverId: String): Boolean {
        return load(context).any { it.serverId == serverId }
    }
}

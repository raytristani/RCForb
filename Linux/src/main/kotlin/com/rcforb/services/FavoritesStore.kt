package com.rcforb.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

@Serializable
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
    private const val KEY = "favorites"
    private val prefs: Preferences = Preferences.userRoot().node("com/rcforb/linux/favorites")
    private val json = Json { ignoreUnknownKeys = true }
    private val listSer = ListSerializer(FavoriteStation.serializer())

    fun save(list: List<FavoriteStation>) {
        prefs.put(KEY, json.encodeToString(listSer, list))
    }

    fun load(): List<FavoriteStation> {
        val raw = prefs.get(KEY, null) ?: return emptyList()
        return try { json.decodeFromString(listSer, raw) } catch (_: Exception) { emptyList() }
    }

    fun addFavorite(station: FavoriteStation) {
        val list = load().toMutableList()
        if (list.none { it.serverId == station.serverId }) {
            list.add(station)
            save(list)
        }
    }

    fun removeFavorite(serverId: String) {
        val list = load().toMutableList()
        list.removeAll { it.serverId == serverId }
        save(list)
    }

    fun isFavorite(serverId: String): Boolean = load().any { it.serverId == serverId }
}

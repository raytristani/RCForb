package com.rcforb.android.services

import com.rcforb.android.models.RemoteStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object LobbyService {
    private const val FEED_URL = "http://online.remotehams.com/xmlfeed.php"
    private val client = OkHttpClient()

    suspend fun fetchStations(): List<RemoteStation> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(FEED_URL).build()
            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: ""
            parseStationXML(xml)
        } catch (e: Exception) {
            android.util.Log.e("LobbyService", "Fetch error", e)
            emptyList()
        }
    }

    private fun parseStationXML(xml: String): List<RemoteStation> {
        val stations = mutableListOf<RemoteStation>()
        val pattern = Pattern.compile("<Radio>([\\s\\S]*?)</Radio>")
        val matcher = pattern.matcher(xml)

        while (matcher.find()) {
            val block = matcher.group(1) ?: continue
            parseRadioBlock(block)?.let { stations.add(it) }
        }
        return stations
    }

    private fun getField(block: String, tag: String): String {
        val pattern = Pattern.compile("<$tag>(.*?)</$tag>", Pattern.DOTALL)
        val matcher = pattern.matcher(block)
        return if (matcher.find()) matcher.group(1)?.trim() ?: "" else ""
    }

    private fun parseRadioBlock(block: String): RemoteStation? {
        val orbId = getField(block, "OrbId")
        val domain = getField(block, "Domain")
        if (orbId.isEmpty() || domain.isEmpty()) return null

        val port = getField(block, "Port").toIntOrNull() ?: 4525
        val voipPort = getField(block, "VoipPort").toIntOrNull() ?: 4524
        val serverName = getField(block, "ServerName").ifEmpty { "Unknown" }

        return RemoteStation(
            serverId = orbId,
            serverName = serverName,
            description = getField(block, "Message"),
            host = domain,
            port = port,
            voipPort = voipPort,
            online = getField(block, "Online").lowercase() == "true",
            radioInUse = false,
            radioOpen = true,
            serverVersion = getField(block, "Version"),
            radioModel = getField(block, "RadioName"),
            country = getField(block, "Country"),
            gridSquare = getField(block, "Grid"),
            latitude = getField(block, "Latitude").toDoubleOrNull() ?: 0.0,
            longitude = getField(block, "Longitude").toDoubleOrNull() ?: 0.0,
            userCount = getField(block, "Users").toIntOrNull() ?: 0,
            maxUsers = getField(block, "MaxUsers").toIntOrNull() ?: 0,
            isV7 = false
        )
    }
}

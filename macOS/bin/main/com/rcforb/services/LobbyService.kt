package com.rcforb.services

import com.rcforb.models.RemoteStation
import com.rcforb.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object LobbyService {
    private const val FEED_URL = "http://online.remotehams.com/xmlfeed.php"

    suspend fun fetchStations(): List<RemoteStation> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/xml")
            }
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            parseStationXML(xml)
        } catch (e: Exception) {
            Log.e("LobbyService", "Fetch error", e)
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

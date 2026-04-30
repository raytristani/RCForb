package com.rcforb.android.models

import java.net.URLDecoder
import java.util.Date

class ServerInfoState {
    var serverId: String = ""
    var serverVersion: String = ""
    var serverUptime: String = ""
    var serverTime: String = ""
    var radioName: String = ""
    var radioDriver: String = ""
    var radioOpen: Boolean = false
    var radioInUse: Boolean = false
    var radioInUseBy: String = ""
    var tot: Int = 180

    fun reset() {
        serverId = ""; serverVersion = ""; serverUptime = ""; serverTime = ""
        radioName = ""; radioDriver = ""
        radioOpen = false; radioInUse = false; radioInUseBy = ""
        tot = 180
    }

    fun processCommand(command: String): ChatMessage? {
        when {
            command.startsWith("post::id::") -> serverId = command.removePrefix("post::id::")
            command.startsWith("post::version::") -> serverVersion = command.removePrefix("post::version::")
            command.startsWith("post::heartbeat::") -> serverUptime = command.removePrefix("post::heartbeat::")
            command.startsWith("post::time::") -> serverTime = command.removePrefix("post::time::")
            command.startsWith("post::lasttuner::") -> radioInUseBy = command.removePrefix("post::lasttuner::")
            command.startsWith("post::tot::") -> tot = command.removePrefix("post::tot::").toIntOrNull() ?: 180
            command.startsWith("post::radio-open") -> { radioOpen = true; radioInUse = false }
            command.startsWith("post::radio-in-use") -> {
                radioInUse = true; radioOpen = false
                val rest = command.removePrefix("post::radio-in-use")
                if (rest.startsWith("::")) radioInUseBy = rest.removePrefix("::")
            }
            command.startsWith("post::radio-closed") -> { radioOpen = false; radioInUse = false }
            command.startsWith("chat::") -> return parseChatMessage(command.removePrefix("chat::"))
        }
        return null
    }

    fun toData(): ServerInfoData = ServerInfoData(
        serverId = serverId, serverVersion = serverVersion,
        serverUptime = serverUptime, serverTime = serverTime,
        radioName = radioName, radioDriver = radioDriver,
        radioOpen = radioOpen, radioInUse = radioInUse,
        radioInUseBy = radioInUseBy, tot = tot
    )

    private fun parseChatMessage(raw: String): ChatMessage {
        val decoded = try {
            URLDecoder.decode(raw, "UTF-8").replace("+", " ")
        } catch (_: Exception) { raw }

        val parts = decoded.split("::")
        var user = ""
        var text = decoded

        if (parts.size >= 3) {
            user = parts[0]
            text = parts.subList(2, parts.size).joinToString("::")
        } else if (parts.size == 2) {
            user = parts[0]
            text = parts[1]
        }

        text = text
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")

        return ChatMessage(
            user = user,
            text = text,
            timestamp = Date(),
            isSystem = user.isEmpty() || user == "System"
        )
    }
}

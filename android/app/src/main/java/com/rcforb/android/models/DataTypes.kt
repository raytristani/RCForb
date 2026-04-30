package com.rcforb.android.models

import java.util.Date
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    AUTHENTICATING,
    AUTHENTICATED,
    CONNECTING,
    CONNECTED,
    FAILED
}

data class RemoteStation(
    val serverId: String,
    val serverName: String,
    val description: String,
    val host: String,
    val port: Int,
    val voipPort: Int,
    val online: Boolean,
    var radioInUse: Boolean,
    var radioOpen: Boolean,
    val serverVersion: String,
    val radioModel: String,
    val country: String,
    val gridSquare: String,
    val latitude: Double,
    val longitude: Double,
    val userCount: Int,
    val maxUsers: Int,
    val isV7: Boolean
)

data class RadioStateData(
    val frequencyA: Int = 0,
    val frequencyB: Int = 0,
    val buttons: Map<String, Int> = emptyMap(),
    val buttonOrder: List<String> = emptyList(),
    val dropdowns: Map<String, String> = emptyMap(),
    val dropdownLists: Map<String, List<String>> = emptyMap(),
    val dropdownOrder: List<String> = emptyList(),
    val sliders: Map<String, Double> = emptyMap(),
    val sliderRanges: Map<String, SliderRange> = emptyMap(),
    val sliderOrder: List<String> = emptyList(),
    val meters: Map<String, MeterData> = emptyMap(),
    val meterOrder: List<String> = emptyList(),
    val messages: Map<String, String> = emptyMap(),
    val messageOrder: List<String> = emptyList(),
    val statuses: Map<String, Boolean> = emptyMap(),
    val statusOrder: List<String> = emptyList(),
    val smeterA: Double = 0.0,
    val smeterALabel: String = "",
    val smeterB: Double = 0.0,
    val smeterBLabel: String = "",
    val txEnabled: Boolean = false
)

data class SliderRange(
    val min: Double,
    val max: Double,
    val step: Double,
    val displayOffset: String
)

data class MeterData(
    val value: Double,
    val max: Double,
    val unit: String
)

data class ServerInfoData(
    val serverId: String = "",
    val serverVersion: String = "",
    val serverUptime: String = "",
    val serverTime: String = "",
    val radioName: String = "",
    val radioDriver: String = "",
    val radioOpen: Boolean = false,
    val radioInUse: Boolean = false,
    val radioInUseBy: String = "",
    val tot: Int = 180
)

data class RotatorStateData(
    val bearing: Int = 0,
    val elevation: Int = 0,
    val moving: Boolean = false,
    val buttons: Map<String, Int> = emptyMap(),
    val buttonOrder: List<String> = emptyList()
)

data class AmpStateData(
    val buttons: Map<String, Int> = emptyMap(),
    val buttonOrder: List<String> = emptyList(),
    val dropdowns: Map<String, String> = emptyMap(),
    val dropdownLists: Map<String, List<String>> = emptyMap(),
    val dropdownOrder: List<String> = emptyList(),
    val sliders: Map<String, Double> = emptyMap(),
    val sliderRanges: Map<String, SliderRange> = emptyMap(),
    val sliderOrder: List<String> = emptyList(),
    val meters: Map<String, MeterData> = emptyMap(),
    val meterOrder: List<String> = emptyList()
)

data class SwitchStateData(
    val buttons: Map<String, Int> = emptyMap(),
    val buttonOrder: List<String> = emptyList()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val user: String,
    val text: String,
    val timestamp: Date = Date(),
    val isSystem: Boolean
)

data class AuthResult(
    val success: Boolean,
    val message: String,
    val apiKey: String? = null
)

data class SavedCredentials(
    val user: String,
    val password: String
)

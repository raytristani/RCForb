package com.rcforb.services

import com.rcforb.audio.AudioBridge
import com.rcforb.audio.CodecType
import com.rcforb.models.*
import com.rcforb.networking.TCPClientV7
import com.rcforb.networking.UDPClient
import com.rcforb.protocol.CommandParser
import com.rcforb.protocol.md5
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Compose-Desktop variant of the Android ConnectionManagerViewModel.
 * Same StateFlow surface; uses a private CoroutineScope (Main + IO).
 * Call cleared() on app shutdown.
 */
class ConnectionManagerViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _stations = MutableStateFlow<List<RemoteStation>>(emptyList())
    val stations: StateFlow<List<RemoteStation>> = _stations

    private val _radioStateData = MutableStateFlow<RadioStateData?>(null)
    val radioStateData: StateFlow<RadioStateData?> = _radioStateData

    private val _serverInfoData = MutableStateFlow<ServerInfoData?>(null)
    val serverInfoData: StateFlow<ServerInfoData?> = _serverInfoData

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _rotatorStateData = MutableStateFlow<RotatorStateData?>(null)
    val rotatorStateData: StateFlow<RotatorStateData?> = _rotatorStateData

    private val _ampStateData = MutableStateFlow<AmpStateData?>(null)
    val ampStateData: StateFlow<AmpStateData?> = _ampStateData

    private val _switchStateData = MutableStateFlow<SwitchStateData?>(null)
    val switchStateData: StateFlow<SwitchStateData?> = _switchStateData

    private val _sliderOverrides = MutableStateFlow<Map<String, Double>>(emptyMap())
    val sliderOverrides: StateFlow<Map<String, Double>> = _sliderOverrides

    private val _connectedStationName = MutableStateFlow("")
    val connectedStationName: StateFlow<String> = _connectedStationName

    private val _connectedStation = MutableStateFlow<RemoteStation?>(null)
    val connectedStation: StateFlow<RemoteStation?> = _connectedStation

    private var udpClient: UDPClient? = null
    private var tcpClient: TCPClientV7? = null
    private var username = ""
    private var passwordMD5 = ""

    private val radioState = RadioState()
    private val serverInfo = ServerInfoState()
    private val rotatorState = RotatorStateModel()
    private val ampState = AmpStateModel()
    private val switchState = SwitchStateModel()
    val audioBridge = AudioBridge()

    private var commandCount = 0

    fun cleared() {
        disconnect()
        scope.cancel()
    }

    // Authentication ---------------------------------------------------------
    fun authenticate(user: String, password: String, onResult: (AuthResult) -> Unit) {
        username = user
        passwordMD5 = md5(password)
        _connectionState.value = ConnectionState.AUTHENTICATING

        scope.launch {
            val result = AuthService.authenticate(user, password)
            if (!result.success) {
                _connectionState.value = ConnectionState.FAILED
            } else {
                _connectionState.value = ConnectionState.AUTHENTICATED
            }
            onResult(result)
        }
    }

    // Lobby ------------------------------------------------------------------
    suspend fun refreshLobby(): List<RemoteStation> {
        val fetched = LobbyService.fetchStations()
        val sorted = fetched
            .filter { it.online }
            .sortedBy { it.serverName.lowercase() }
        _stations.value = sorted
        return sorted
    }

    // Connect ----------------------------------------------------------------
    fun connectToStation(station: RemoteStation) {
        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            _connectedStationName.value = station.serverName
            _connectedStation.value = station
            commandCount = 0

            val host = station.host
            val port = station.port

            val udpOk = tryV10(host, port)

            if (udpOk) {
                audioBridge.start(CodecType.OPUS)
                audioBridge.onEncodedAudio = { data -> udpClient?.sendAudio(data) }
            } else {
                val tcpOk = tryV7(host, port, station.voipPort)
                if (!tcpOk) {
                    _connectionState.value = ConnectionState.AUTHENTICATED
                    _errorMessage.value = "Could not connect to ${station.serverName}"
                    return@launch
                }
                audioBridge.start(CodecType.SPEEX)
                audioBridge.onEncodedAudio = { data -> tcpClient?.sendAudio(data) }
            }

            sendCommand("get id")
            sendCommand(CommandParser.loginCmd(username, passwordMD5))
            withContext(Dispatchers.IO) {
                tcpClient?.sendSessionLogin(username, passwordMD5)
            }
            sendCommand(CommandParser.setProtocolRCS())
            sendCommand(CommandParser.requestRadioState())

            _connectionState.value = ConnectionState.CONNECTED

            launch { AuthService.trackOnline(username, passwordMD5, station.serverId) }
        }
    }

    private suspend fun tryV10(host: String, port: Int): Boolean {
        val udp = UDPClient()
        udp.onAudio = { data -> audioBridge.pushRXAudio(data) }
        udp.onCommand = { text ->
            scope.launch { dispatchCommand(text) }
        }
        udp.onControl = { byte ->
            scope.launch { handleControlByte(byte) }
        }
        udp.onDisconnected = {
            scope.launch {
                if (udpClient === udp) _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        val connected = udp.connect(host, port)
        if (!connected) return false

        val flowing = udp.waitForDataFlow(3000)
        if (!flowing) {
            udp.disconnect()
            return false
        }

        udpClient = udp
        tcpClient = null
        return true
    }

    private suspend fun tryV7(host: String, port: Int, voipPort: Int): Boolean {
        val tcp = TCPClientV7(if (voipPort > 0) voipPort else 4524)
        tcp.onAudio = { data -> audioBridge.pushRXAudio(data) }
        tcp.onCommand = { text ->
            scope.launch { dispatchCommand(text) }
        }
        tcp.onControl = { byte ->
            scope.launch { handleControlByte(byte) }
        }
        tcp.onDisconnected = {
            scope.launch {
                if (tcpClient === tcp) _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        val connected = tcp.connect(host, port)
        if (!connected) return false

        val flowing = tcp.waitForDataFlow(5000)
        if (!flowing) {
            tcp.disconnect()
            return false
        }

        tcpClient = tcp
        udpClient = null
        return true
    }

    fun disconnect() {
        audioBridge.stop()
        val udp = udpClient; val tcp = tcpClient
        udpClient = null; tcpClient = null
        scope.launch(Dispatchers.IO) {
            udp?.disconnect(); tcp?.disconnect()
        }
        radioState.reset(); serverInfo.reset()
        rotatorState.reset(); ampState.reset(); switchState.reset()
        _radioStateData.value = null; _serverInfoData.value = null
        _rotatorStateData.value = null; _ampStateData.value = null; _switchStateData.value = null
        _chatMessages.value = emptyList()
        _connectedStationName.value = ""
        _connectedStation.value = null
        _connectionState.value = ConnectionState.AUTHENTICATED
    }

    fun logout() {
        disconnect()
        username = ""; passwordMD5 = ""
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // Send commands ----------------------------------------------------------
    fun sendCommand(command: String) {
        scope.launch(Dispatchers.IO) {
            tcpClient?.sendCommandString(command)
            udpClient?.sendCommandString(command)
        }
    }

    private fun getTXButton(): String? {
        val buttons = _radioStateData.value?.buttons ?: return null
        if ("TX" in buttons) return "TX"
        if ("TXd" in buttons) return "TXd"
        return null
    }

    fun sendPTT(on: Boolean) {
        val txButton = getTXButton()
        if (txButton != null) {
            sendCommand(CommandParser.setButton(txButton, if (on) "1" else "0"))
        }
        scope.launch(Dispatchers.IO) {
            tcpClient?.sendPTT(on)
            udpClient?.sendPTT(on)
        }
        if (on) audioBridge.startTX() else audioBridge.stopTX()
    }

    fun clearError() { _errorMessage.value = null }
    fun clearSliderOverrides() { _sliderOverrides.value = emptyMap() }
    fun setSliderOverride(name: String, value: Double) {
        _sliderOverrides.value = _sliderOverrides.value + (name to value)
    }

    // Dispatch ---------------------------------------------------------------
    private fun dispatchCommand(command: String) {
        commandCount++
        val radioCommand = translateV7Command(command)

        if (radioCommand != null) {
            val changed = radioState.processCommand(radioCommand)
            if (changed) _radioStateData.value = radioState.toData()
        } else if (command.startsWith("radio::") || command.startsWith("chat::")) {
            val changed = radioState.processCommand(command)
            if (changed) _radioStateData.value = radioState.toData()
        }

        if (command.startsWith("post::") || command.startsWith("chat::") ||
            command.startsWith("mem::") || command.startsWith("log::")) {
            val chatMsg = serverInfo.processCommand(command)
            _serverInfoData.value = serverInfo.toData()
            if (chatMsg != null) {
                val msgs = _chatMessages.value.toMutableList()
                msgs.add(chatMsg)
                if (msgs.size > 200) msgs.removeAt(0)
                _chatMessages.value = msgs
            }
        }

        if (command.startsWith("rotator::")) {
            rotatorState.processCommand(command)
            _rotatorStateData.value = rotatorState.toData()
        }
        if (command.startsWith("amp::")) {
            ampState.processCommand(command)
            _ampStateData.value = ampState.toData()
        }
        if (command.startsWith("switch::")) {
            switchState.processCommand(command)
            _switchStateData.value = switchState.toData()
        }
    }

    private fun translateV7Command(command: String): String? {
        if (!command.startsWith("post::")) return null
        return command.replace("post::", "radio::")
    }

    private fun handleControlByte(byte: Byte) {
        when (byte) {
            com.rcforb.protocol.ControlByte.PTT -> {
                _radioStateData.value = radioState.toData().copy(txEnabled = true)
                audioBridge.startTX()
            }
            com.rcforb.protocol.ControlByte.PTT_OFF -> {
                _radioStateData.value = radioState.toData().copy(txEnabled = false)
                audioBridge.stopTX()
            }
        }
    }
}

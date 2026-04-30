package com.rcforb.android.networking

import android.util.Log
import com.rcforb.android.protocol.*
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class UDPClient {
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private var port: Int = 0
    private var isConnectedFlag = false
    private var isDataFlowingFlag = false
    private var lastServerData = System.currentTimeMillis()
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var pingJob: Job? = null
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onAudio: ((ByteArray) -> Unit)? = null
    var onCommand: ((String) -> Unit)? = null
    var onControl: ((Byte) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    private var dataFlowContinuation: CancellableContinuation<Boolean>? = null

    val isConnected: Boolean get() = isConnectedFlag
    val isDataFlowing: Boolean get() = isDataFlowingFlag

    suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val addr = InetAddress.getByName(host)
            val sock = DatagramSocket()
            sock.soTimeout = 500
            sock.connect(addr, port)

            this@UDPClient.socket = sock
            this@UDPClient.address = addr
            this@UDPClient.port = port
            isConnectedFlag = true
            isDataFlowingFlag = false
            lastServerData = System.currentTimeMillis()

            // Send USERIN x3
            repeat(3) { sendRaw(byteArrayOf(ControlByte.USERIN)) }

            startHeartbeat()
            startReceiving()
            true
        } catch (e: Exception) {
            Log.e("UDPClient", "Connect failed", e)
            false
        }
    }

    fun disconnect() {
        if (!isConnectedFlag && socket == null) return
        sendRaw(byteArrayOf(ControlByte.USEROUT))
        stopAll()
        onDisconnected?.invoke()
    }

    private fun handleDisconnect() {
        stopAll()
        onDisconnected?.invoke()
    }

    private fun stopAll() {
        isConnectedFlag = false
        isDataFlowingFlag = false
        receiveJob?.cancel()
        heartbeatJob?.cancel()
        pingJob?.cancel()
        timeoutJob?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    fun sendRaw(data: ByteArray) {
        val sock = socket ?: return
        val addr = address ?: return
        try {
            val packet = DatagramPacket(data, data.size, addr, port)
            sock.send(packet)
        } catch (_: Exception) {}
    }

    fun sendCommandString(text: String) {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val packet = ByteArray(1 + textBytes.size)
        packet[0] = ControlByte.UTF8STRING
        System.arraycopy(textBytes, 0, packet, 1, textBytes.size)
        sendRaw(packet)
    }

    fun sendPTT(on: Boolean) {
        sendRaw(byteArrayOf(if (on) ControlByte.PTT else ControlByte.PTT_OFF))
    }

    fun sendAudio(data: ByteArray) {
        sendRaw(data)
    }

    private fun startReceiving() {
        receiveJob = scope.launch {
            val buf = ByteArray(65536)
            while (isActive && isConnectedFlag) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket?.receive(packet)
                    if (packet.length > 0) {
                        val data = buf.copyOfRange(0, packet.length)
                        processPacket(data)
                    }
                } catch (_: SocketTimeoutException) {
                    // Normal — just retry
                } catch (e: Exception) {
                    if (isConnectedFlag) {
                        Log.e("UDPClient", "Receive error", e)
                        handleDisconnect()
                    }
                    break
                }
            }
        }
    }

    private fun processPacket(data: ByteArray) {
        if (data.isEmpty()) return

        lastServerData = System.currentTimeMillis()
        if (!isDataFlowingFlag) {
            isDataFlowingFlag = true
            dataFlowContinuation?.resumeWith(Result.success(true))
            dataFlowContinuation = null
        }

        when (classifyPacket(data)) {
            PacketType.AUDIO -> onAudio?.invoke(data)
            PacketType.COMMAND -> {
                val text = String(data, 1, data.size - 1, Charsets.UTF_8)
                onCommand?.invoke(text)
            }
            PacketType.PTT_ON -> onControl?.invoke(ControlByte.PTT)
            PacketType.PTT_OFF -> onControl?.invoke(ControlByte.PTT_OFF)
            PacketType.KEY_ON -> onControl?.invoke(ControlByte.KEY_ON)
            PacketType.KEY_OFF -> onControl?.invoke(ControlByte.KEY_OFF)
            PacketType.USER_OUT -> handleDisconnect()
            PacketType.HEARTBEAT, PacketType.PINGPONG, PacketType.USER_IN -> {}
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive && isConnectedFlag) {
                sendRaw(byteArrayOf(ControlByte.HEARTBEAT))
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
        pingJob = scope.launch {
            while (isActive && isConnectedFlag) {
                sendRaw(byteArrayOf(ControlByte.PINGPONG))
                delay(PING_INTERVAL_MS)
            }
        }
        timeoutJob = scope.launch {
            while (isActive && isConnectedFlag) {
                delay(1000)
                if (System.currentTimeMillis() - lastServerData > HEARTBEAT_TIMEOUT_MS) {
                    Log.w("UDPClient", "Heartbeat timeout, disconnecting")
                    handleDisconnect()
                    break
                }
            }
        }
    }

    suspend fun waitForDataFlow(timeoutMs: Long = 8000): Boolean {
        if (isDataFlowingFlag) return true
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                dataFlowContinuation = cont
            }
        } ?: false
    }
}

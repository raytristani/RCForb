package com.rcforb.android.networking

import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class IpExClient {
    companion object {
        private const val IPEX_HOST = "ipex.remotehams.com"
        private const val IPEX_PORT = 7005
    }

    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var isConnectedFlag = false
    private var lastDataTime = System.currentTimeMillis()
    private var lineBuffer = StringBuilder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var jobs = mutableListOf<Job>()

    var onHolePunchGo: ((String) -> Unit)? = null
    var onHolePunchOk: (() -> Unit)? = null
    var onHolePunchFail: (() -> Unit)? = null
    var onServerNotFound: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    val isConnected: Boolean get() = isConnectedFlag

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val sock = Socket(IPEX_HOST, IPEX_PORT)
            sock.soTimeout = 500
            socket = sock
            output = sock.getOutputStream()
            isConnectedFlag = true
            lastDataTime = System.currentTimeMillis()
            startHeartbeat()
            startReceiving()
            true
        } catch (e: Exception) {
            Log.e("IpExClient", "Connect failed", e)
            false
        }
    }

    fun disconnect() {
        isConnectedFlag = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        try { socket?.close() } catch (_: Exception) {}
        socket = null; output = null
        onDisconnected?.invoke()
    }

    private fun handleDisconnect() {
        if (!isConnectedFlag) return
        isConnectedFlag = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        try { socket?.close() } catch (_: Exception) {}
        socket = null; output = null
        onDisconnected?.invoke()
    }

    fun holePunchRequest(serverEndpoint: String, clientPort: Int) {
        send("ClientConnectRequest,$serverEndpoint,$clientPort\n")
    }

    fun connectRequestCompleted() = send("ClientConnectRequest,OK\n")
    fun connectRequestFailed() = send("ClientConnectRequest,FAIL\n")

    private fun send(data: String) {
        if (!isConnectedFlag) return
        try { output?.write(data.toByteArray(Charsets.UTF_8)) } catch (_: Exception) {}
    }

    private fun startReceiving() {
        val job = scope.launch {
            val buf = ByteArray(65536)
            val input: InputStream = socket?.getInputStream() ?: return@launch
            while (isActive && isConnectedFlag) {
                try {
                    val len = input.read(buf)
                    if (len > 0) processData(String(buf, 0, len, Charsets.UTF_8))
                    else if (len == -1) { handleDisconnect(); break }
                } catch (_: java.net.SocketTimeoutException) {
                } catch (_: Exception) { if (isConnectedFlag) handleDisconnect(); break }
            }
        }
        jobs.add(job)
    }

    private fun processData(data: String) {
        lastDataTime = System.currentTimeMillis()
        lineBuffer.append(data)
        val lines = lineBuffer.toString().split("\n").toMutableList()
        lineBuffer = StringBuilder(lines.removeAt(lines.size - 1))
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) processLine(trimmed)
        }
    }

    private fun processLine(line: String) {
        when {
            line.startsWith("ClientConnectRequest,GO") -> {
                val parts = line.split(",")
                val endpoint = if (parts.size > 2) parts[2] else ""
                onHolePunchGo?.invoke(endpoint)
            }
            line == "ClientConnectRequest,OK" -> onHolePunchOk?.invoke()
            line == "ClientConnectRequest,FAIL" -> onHolePunchFail?.invoke()
            line.startsWith("ServerNotFound") -> onServerNotFound?.invoke()
        }
    }

    private fun startHeartbeat() {
        val hbJob = scope.launch {
            while (isActive && isConnectedFlag) {
                send("${System.currentTimeMillis()}\n")
                delay(4000)
            }
        }
        jobs.add(hbJob)

        val toJob = scope.launch {
            while (isActive && isConnectedFlag) {
                delay(1000)
                if (System.currentTimeMillis() - lastDataTime > 15000) {
                    Log.w("IpExClient", "Heartbeat timeout")
                    handleDisconnect()
                    break
                }
            }
        }
        jobs.add(toJob)
    }
}

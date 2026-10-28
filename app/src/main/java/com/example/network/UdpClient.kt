package com.example.network

import android.util.Log
import com.example.model.RemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpClient(
    private val deviceId: String,
    private val deviceName: String,
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideUdpClient"

    @Volatile
    private var socket: DatagramSocket? = null
    @Volatile
    private var targetAddress: InetAddress? = null
    @Volatile
    private var targetPort: Int = 5055

    private val _packetsSent = MutableStateFlow(0L)
    val packetsSent = _packetsSent.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    // Sequential non-blocking packet queue - Eliminates 120Hz coroutine churn & ensures strict FIFO order
    private val packetChannel = Channel<String>(capacity = 256)

    private var workerJob: Job? = null
    private var heartbeatJob: Job? = null

    init {
        initSocket()
        startWorkerLoop()
    }

    @Synchronized
    private fun initSocket() {
        try {
            if (socket == null || socket?.isClosed == true) {
                socket = DatagramSocket().apply {
                    broadcast = true
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize DatagramSocket", e)
            _lastError.value = e.localizedMessage
        }
    }

    private fun startWorkerLoop() {
        workerJob?.cancel()
        workerJob = scope.launch(Dispatchers.IO) {
            for (payload in packetChannel) {
                val address = targetAddress ?: continue
                try {
                    val currentSocket = socket ?: DatagramSocket().apply { broadcast = true }.also { socket = it }
                    val bytes = payload.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(bytes, bytes.size, address, targetPort)
                    currentSocket.send(packet)
                    _packetsSent.value += 1
                } catch (e: Exception) {
                    Log.w(tag, "Send error: ${e.message}")
                    _lastError.value = "Send error: ${e.localizedMessage}"
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }
            }
        }
    }

    fun configureTarget(hostIp: String, port: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                targetPort = port
                val cleanIp = hostIp.trim()
                if (cleanIp.isNotEmpty()) {
                    val resolved = InetAddress.getByName(cleanIp)
                    targetAddress = resolved
                    _lastError.value = null
                    Log.i(tag, "Target configured: $resolved:$port")
                    startHeartbeat()
                }
            } catch (e: Exception) {
                Log.e(tag, "Invalid target host $hostIp:$port", e)
                _lastError.value = "Invalid IP: ${e.message}"
            }
        }
    }

    fun sendCommand(command: RemoteCommand) {
        val payload = command.toWireString(deviceId)
        sendRaw(payload)
    }

    fun sendRaw(payload: String) {
        // Enqueue directly without launching coroutine on UI thread
        packetChannel.trySend(payload)
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                sendCommand(RemoteCommand.Presence(deviceId, deviceName))
                delay(2000)
            }
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        workerJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}

package com.example.network

import android.util.Log
import com.example.model.RemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Sends mouse/keyboard commands to the laptop over UDP and listens for the
 * laptop's "AIRGLIDE_ACK" replies, which prove that the laptop really receives us.
 */
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

    /** Wall-clock time of the last reply from the laptop (0 = never). */
    @Volatile
    var lastAckMs: Long = 0L
        private set

    private val _packetsSent = MutableStateFlow(0L)
    val packetsSent: StateFlow<Long> = _packetsSent.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // One ordered queue -> strict FIFO delivery, no coroutine per pointer move.
    private val packetChannel = Channel<String>(capacity = 256)

    private var workerJob: Job? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null

    init {
        startWorkerLoop()
        startReceiveLoop()
    }

    @Synchronized
    private fun obtainSocket(): DatagramSocket {
        val existing = socket
        if (existing != null && !existing.isClosed) return existing
        val created = DatagramSocket()
        created.soTimeout = 1000
        socket = created
        return created
    }

    @Synchronized
    private fun dropSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun startWorkerLoop() {
        workerJob?.cancel()
        workerJob = scope.launch(Dispatchers.IO) {
            for (payload in packetChannel) {
                val address = targetAddress ?: continue
                try {
                    val bytes = payload.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(bytes, bytes.size, address, targetPort)
                    obtainSocket().send(packet)
                    _packetsSent.value += 1
                } catch (e: Exception) {
                    Log.w(tag, "Send error: ${e.message}")
                    _lastError.value = "Send error: ${e.localizedMessage}"
                    dropSocket()
                }
            }
        }
    }

    private fun startReceiveLoop() {
        receiveJob?.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(512)
            while (isActive) {
                val s = socket
                if (s == null || s.isClosed) {
                    delay(250)
                    continue
                }
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val fromTarget = packet.address?.hostAddress == targetAddress?.hostAddress
                    if (fromTarget && msg.startsWith("AIRGLIDE_ACK")) {
                        lastAckMs = System.currentTimeMillis()
                    }
                } catch (_: SocketTimeoutException) {
                    // normal: nothing received within 1 s
                } catch (_: Exception) {
                    delay(200)
                }
            }
        }
    }

    fun clearTarget() {
        targetAddress = null
        lastAckMs = 0L
        heartbeatJob?.cancel()
    }

    fun configureTarget(hostIp: String, port: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val cleanIp = hostIp.trim()
                if (cleanIp.isEmpty()) {
                    clearTarget()
                    return@launch
                }
                val resolved = InetAddress.getByName(cleanIp)
                val changed = resolved.hostAddress != targetAddress?.hostAddress || port != targetPort
                targetPort = port
                targetAddress = resolved
                if (changed) lastAckMs = 0L
                _lastError.value = null
                Log.i(tag, "Target configured: $resolved:$port")
                startHeartbeat()
            } catch (e: Exception) {
                Log.e(tag, "Invalid target host $hostIp:$port", e)
                _lastError.value = "Invalid IP: ${e.message}"
            }
        }
    }

    fun sendCommand(command: RemoteCommand) {
        sendRaw(command.toWireString(deviceId))
    }

    fun sendRaw(payload: String) {
        packetChannel.trySend(payload)
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                sendCommand(RemoteCommand.Presence(deviceId, deviceName))
                delay(1500)
            }
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        workerJob?.cancel()
        receiveJob?.cancel()
        dropSocket()
    }
}

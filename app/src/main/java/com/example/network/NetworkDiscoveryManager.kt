package com.example.network

import android.content.Context
import android.util.Log
import com.example.model.DiscoveredHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * Finds laptops running the AirGlide receiver.
 *
 *  - The receiver broadcasts "AIRGLIDE_BEACON:<host>:<ip>:<port>" to UDP port 5056.
 *  - We also broadcast "AIRGLIDE_DISCOVERY_PING" to port 5055; the receiver answers
 *    with "AIRGLIDE_SERVER_OFFER:<host>:<ip>:<port>" straight to our socket.
 *
 * IMPORTANT: the laptop's address is always taken from the packet's real source
 * address, never from the text inside the packet. The advertised text can name a
 * virtual adapter (VirtualBox / WSL / VPN) that the phone cannot reach.
 */
class NetworkDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideDiscovery"

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private var mainJob: Job? = null

    @Volatile
    private var socket: DatagramSocket? = null

    fun start() {
        if (mainJob?.isActive == true) return
        NetworkUtils.acquireMulticastLock(context)

        mainJob = scope.launch(Dispatchers.IO) {
            val s = openSocket()
            socket = s

            // Periodic ping + removal of laptops that stopped answering.
            launch {
                while (isActive) {
                    sendDiscoveryPing()
                    cleanStaleHosts()
                    delay(3000)
                }
            }

            val buffer = ByteArray(1024)
            try {
                while (isActive && !s.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    s.receive(packet) // blocks; stop() closes the socket to unblock it
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    val senderIp = packet.address?.hostAddress ?: ""
                    processDiscoveryMessage(msg, senderIp)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(tag, "Discovery listener closed: ${e.message}")
                }
            }
        }
    }

    private fun openSocket(): DatagramSocket {
        return try {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(DISCOVERY_PORT))
            }
        } catch (_: Exception) {
            // Port 5056 busy: still fine, ping/offer replies reach this ephemeral socket.
            DatagramSocket().apply { broadcast = true }
        }
    }

    fun triggerScan() {
        scope.launch(Dispatchers.IO) {
            sendDiscoveryPing()
        }
    }

    private fun sendDiscoveryPing() {
        val s = socket ?: return
        val pingMsg = "AIRGLIDE_DISCOVERY_PING".toByteArray(Charsets.UTF_8)
        for (address in NetworkUtils.getBroadcastAddresses()) {
            try {
                s.send(DatagramPacket(pingMsg, pingMsg.size, address, DEFAULT_PORT))
            } catch (e: Exception) {
                Log.d(tag, "Ping to $address failed: ${e.message}")
            }
        }
    }

    private fun processDiscoveryMessage(msg: String, senderIp: String) {
        if (senderIp.isBlank()) return
        if (!msg.startsWith("AIRGLIDE_BEACON:") && !msg.startsWith("AIRGLIDE_SERVER_OFFER:")) return

        val parts = msg.split(":")
        if (parts.size < 4) return

        val hostName = parts[1].ifBlank { "Laptop" }
        val port = parts.last().toIntOrNull() ?: DEFAULT_PORT

        val host = DiscoveredHost(
            hostName = hostName,
            ipAddress = senderIp,
            port = port,
            lastSeenMs = System.currentTimeMillis()
        )

        val current = _discoveredHosts.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.ipAddress == senderIp && it.port == port }
        if (existingIndex >= 0) {
            current[existingIndex] = host
        } else {
            current.add(host)
            Log.i(tag, "Discovered AirGlide host: $hostName ($senderIp:$port)")
        }
        _discoveredHosts.value = current
    }

    private fun cleanStaleHosts() {
        val now = System.currentTimeMillis()
        val current = _discoveredHosts.value
        val fresh = current.filter { (now - it.lastSeenMs) < STALE_AFTER_MS }
        if (fresh.size != current.size) {
            _discoveredHosts.value = fresh
        }
    }

    fun stop() {
        mainJob?.cancel()
        mainJob = null
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        NetworkUtils.releaseMulticastLock()
    }

    private companion object {
        const val DEFAULT_PORT = 5055
        const val DISCOVERY_PORT = 5056
        const val STALE_AFTER_MS = 8000L
    }
}

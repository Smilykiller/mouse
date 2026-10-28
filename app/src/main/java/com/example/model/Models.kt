package com.example.model

enum class AppScreen {
    HUB_SETTINGS,     // First screen on entering the app with all sensitivities and connectivity option
    TRACKPAD,         // The active mouse/scroll/touchpad screen
    DEVICE_CONNECTION // Dedicated screen for device connectivity, connection status, Wi-Fi/BT checks & history
}

enum class TouchpadLayout(val label: String, val description: String) {
    SPLIT("Split Layout", "Large trackpad + dedicated scroll strip + twin click boxes"),
    CLASSIC("Classic Laptop", "Full-width trackpad with dual bottom click buttons"),
    GESTURE("Gesture Surface", "Full-screen gesture pad with tap-to-click & two-finger scroll")
}

enum class AppRole(val label: String) {
    CONTROLLER("Mobile Controller"),
    HOST("Host Receiver")
}

enum class ConnectionMode(val label: String) {
    WIFI_UDP("Wi-Fi / Hotspot (UDP)"),
    BLUETOOTH("Bluetooth")
}

enum class ClickButton(val code: String, val label: String) {
    LEFT("L", "Left Click"),
    RIGHT("R", "Right Click"),
    MIDDLE("M", "Middle Click")
}

sealed class RemoteCommand {
    data class Move(val dx: Float, val dy: Float) : RemoteCommand()
    data class Click(val button: ClickButton) : RemoteCommand()
    data class MouseDown(val button: ClickButton) : RemoteCommand()
    data class MouseUp(val button: ClickButton) : RemoteCommand()
    data class Scroll(val delta: Float) : RemoteCommand()
    data class Key(val keyName: String) : RemoteCommand()
    data class Text(val content: String) : RemoteCommand()
    data class Presence(val deviceId: String, val deviceName: String) : RemoteCommand()
    object Disconnect : RemoteCommand()

    fun toWireString(deviceId: String): String {
        return when (this) {
            is Move -> "M:${Math.round(dx)},${Math.round(dy)}:$deviceId"
            is Click -> "C:${button.code}:$deviceId"
            is MouseDown -> "D:${button.code}:$deviceId"
            is MouseUp -> "U:${button.code}:$deviceId"
            is Scroll -> "S:${Math.round(delta)}:$deviceId"
            is Key -> "K:$keyName:$deviceId"
            is Text -> "T:${content.replace("\\", "\\\\").replace(":", "\\:").replace("\n", "\\n")}:$deviceId"
            is Presence -> "P:$deviceId:${deviceName.replace(":", "-")}"
            is Disconnect -> "X:bye:$deviceId"
        }
    }
}

data class DiscoveredHost(
    val hostName: String,
    val ipAddress: String,
    val port: Int = 5055,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class ConnectedDevice(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val lastSeenMs: Long = System.currentTimeMillis(),
    val packetsReceived: Long = 0L,
    val isActiveSelected: Boolean = false
)

data class DeviceHistoryItem(
    val name: String,
    val ipAddress: String,
    val port: Int = 5055,
    val lastUsedMs: Long = System.currentTimeMillis()
)

data class AppSettings(
    val targetHostIp: String = "192.168.1.100",
    val targetPort: Int = 5055,
    val sensitivity: Float = 1.3f,
    val scrollSensitivity: Float = 1.0f,
    val invertScroll: Boolean = false,
    val hapticFeedback: Boolean = true,
    val smoothCursor: Boolean = true,
    val activeDeviceId: String = ""
)

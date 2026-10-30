package com.example.model

enum class AppScreen {
    HUB_SETTINGS,     // First screen on entering the app with all sensitivities and connectivity option
    TRACKPAD,         // The active mouse/scroll/touchpad screen
    DEVICE_CONNECTION // Dedicated screen for device connectivity, connection status, Wi-Fi/BT checks & history
}

enum class AppNavTab(val title: String) {
    TRACKPAD("Mousepad"),
    THEMES("Themes"),
    HELP("Help"),
    SETTINGS("Settings")
}

enum class MousePadTheme(
    val id: String,
    val title: String,
    val subtitle: String
) {
    STEALTH("stealth", "Stealth Dark", "Matte graphite with precision tech grid"),
    CYBER_NEON("cyber_neon", "Cyber Neon", "Electric cyan & neon glow"),
    FROSTED_GLASS("frosted_glass", "Frosted Glass", "Translucent glass with luminous edges"),
    CARBON_FIBER("carbon_fiber", "Carbon Fiber", "Woven composite sports trackpad"),
    SUNSET_AURA("sunset_aura", "Sunset Aura", "Cosmic violet & warm magenta glow"),
    EMERALD_MATRIX("emerald_matrix", "Emerald Matrix", "Glowing matrix terminal theme"),
    RETRO_SYNTHWAVE("retro_synthwave", "Retro Synthwave", "80s neon grid with magenta horizon"),
    NORDIC_MINIMALIST("nordic_minimalist", "Nordic Slate", "Scandi matte birch & calm slate"),
    TITANIUM_METAL("titanium_metal", "Brushed Titanium", "Industrial alloy with precision crosshairs"),
    DEEP_SPACE("deep_space", "Cosmic Nebula", "Deep space galaxy dust & stellar glow"),
    CUSTOM_PHOTO("custom_photo", "Custom Photo", "Your personalized gallery picture")
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

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean = true
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

data class KeyItem(
    val code: String,
    val label: String,
    val category: String = "Common"
)

object KeyboardCatalog {
    val ALL_KEYS: List<KeyItem> = listOf(
        // Modifiers & Controls
        KeyItem("ESC", "ESC", "Controls"),
        KeyItem("TAB", "TAB", "Controls"),
        KeyItem("SHIFT", "SHIFT", "Controls"),
        KeyItem("CTRL", "CTRL", "Controls"),
        KeyItem("ALT", "ALT", "Controls"),
        KeyItem("WIN", "WIN", "Controls"),
        KeyItem("ENTER", "ENTER", "Controls"),
        KeyItem("CAPSLOCK", "CAPS", "Controls"),

        // Navigation
        KeyItem("HOME", "HOME", "Navigation"),
        KeyItem("END", "END", "Navigation"),
        KeyItem("PAGE_UP", "PGUP", "Navigation"),
        KeyItem("PAGE_DOWN", "PGDN", "Navigation"),
        KeyItem("UP", "▲", "Navigation"),
        KeyItem("DOWN", "▼", "Navigation"),
        KeyItem("LEFT", "◄", "Navigation"),
        KeyItem("RIGHT", "►", "Navigation"),

        // Editing
        KeyItem("DELETE", "DEL", "Editing"),
        KeyItem("INSERT", "INS", "Editing"),
        KeyItem("BACKSPACE", "BKSP", "Editing"),
        KeyItem("SPACE", "SPACE", "Editing"),

        // Function Keys
        KeyItem("F1", "F1", "Function"),
        KeyItem("F2", "F2", "Function"),
        KeyItem("F3", "F3", "Function"),
        KeyItem("F4", "F4", "Function"),
        KeyItem("F5", "F5", "Function"),
        KeyItem("F6", "F6", "Function"),
        KeyItem("F7", "F7", "Function"),
        KeyItem("F8", "F8", "Function"),
        KeyItem("F9", "F9", "Function"),
        KeyItem("F10", "F10", "Function"),
        KeyItem("F11", "F11", "Function"),
        KeyItem("F12", "F12", "Function"),
        KeyItem("PRINTSCREEN", "PRTSC", "Function")
    )

    val DEFAULT_KEY_CODES: List<String> = listOf(
        "ESC", "TAB", "SHIFT", "ENTER",
        "HOME", "END", "UP", "DOWN"
    )

    fun find(code: String): KeyItem {
        return ALL_KEYS.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: KeyItem(code, code, "Custom")
    }
}

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRole
import com.example.model.BluetoothDeviceInfo
import com.example.model.ConnectionMode
import com.example.model.DeviceHistoryItem
import com.example.model.DiscoveredHost
import com.example.network.NetworkUtils
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotAmberMild
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.DotRedMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState

@Composable
fun DeviceConnectivityScreen(
    state: AirGlideUiState,
    onBack: () -> Unit,
    onConnectToIp: (String, Int) -> Unit,
    onSelectDiscoveredHost: (DiscoveredHost) -> Unit,
    onSelectHistoryDevice: (DeviceHistoryItem) -> Unit,
    onDeleteHistoryItem: (DeviceHistoryItem) -> Unit,
    onSelectHostActiveDevice: (String) -> Unit,
    onOpenCompanionScript: () -> Unit,
    onRefreshHardware: () -> Unit,
    onDisconnect: () -> Unit = {},
    onSelectConnectionMode: (ConnectionMode) -> Unit = {},
    onScanBluetooth: () -> Unit = {},
    onConnectBluetooth: (BluetoothDeviceInfo) -> Unit = {},
    onDisconnectBluetooth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var inputIp by remember { mutableStateOf(state.settings.targetHostIp) }
    var inputPort by remember { mutableStateOf(state.settings.targetPort.toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // App Bar
        Surface(
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("connectivity_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Settings",
                        tint = WhitePrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Connectivity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary
                    )
                    Text(
                        text = if (state.role == AppRole.CONTROLLER) "Mobile Client Manager" else "Host Receiver Hub",
                        fontSize = 11.sp,
                        color = GraySecondary
                    )
                }

                IconButton(onClick = onRefreshHardware) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan / Refresh", tint = WhitePrimary)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mobile Connection Status Banner (Accurate True Connection State)
            if (state.role == AppRole.CONTROLLER) {
                val statusColor = when {
                    state.isLiveConnected -> DotGreenMild
                    state.isLocalNetworkActive -> DotAmberMild
                    else -> DotRedMild
                }
                val statusText = when {
                    state.isLiveConnected -> "LIVE CONNECTED"
                    state.isLocalNetworkActive -> "STREAMING READY"
                    else -> "DISCONNECTED"
                }

                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CONNECTION STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrayMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhitePrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.isLiveConnected) {
                                    if (state.connectionMode == ConnectionMode.BLUETOOTH)
                                        "Bluetooth Linked: ${state.activeHostName}"
                                    else
                                        "Linked: ${state.activeHostName} (${state.settings.targetHostIp}:${state.settings.targetPort})"
                                } else if (state.connectionMode == ConnectionMode.BLUETOOTH) {
                                    "Bluetooth: ${state.bluetoothStatus}"
                                } else if (state.isLocalNetworkActive) {
                                    "Target: ${state.settings.targetHostIp}:${state.settings.targetPort} (Open Laptop App)"
                                } else {
                                    "Wi-Fi or Bluetooth connection required"
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GraySecondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElevatedSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                            ) {
                                Text(
                                    text = "${state.packetsSent} pkts",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = WhitePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (state.isLiveConnected || state.settings.targetHostIp.isNotBlank()) {
                                Button(
                                    onClick = onDisconnect,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2D1414),
                                        contentColor = Color(0xFFF87171)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Disconnect",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF87171)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Connection Mode Switcher (Wi-Fi vs Bluetooth)
            if (state.role == AppRole.CONTROLLER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Wi-Fi Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.connectionMode == ConnectionMode.WIFI_UDP) Color(0xFF131A26) else CardDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (state.connectionMode == ConnectionMode.WIFI_UDP) Color(0xFF38BDF8) else BorderDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectConnectionMode(ConnectionMode.WIFI_UDP) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (state.connectionMode == ConnectionMode.WIFI_UDP) Color(0xFF38BDF8) else GrayMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Wi-Fi / Hotspot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                                Text("Ultra-fast UDP", fontSize = 10.sp, color = GraySecondary)
                            }
                        }
                    }

                    // Bluetooth Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (state.connectionMode == ConnectionMode.BLUETOOTH) Color(0xFF131A26) else CardDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (state.connectionMode == ConnectionMode.BLUETOOTH) Color(0xFF38BDF8) else BorderDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectConnectionMode(ConnectionMode.BLUETOOTH)
                                onScanBluetooth()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (state.connectionMode == ConnectionMode.BLUETOOTH) Color(0xFF38BDF8) else GrayMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Bluetooth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                                Text("Zero Wi-Fi", fontSize = 10.sp, color = GraySecondary)
                            }
                        }
                    }
                }
            }

            // Bluetooth Devices List (Shown when Bluetooth Mode is selected)
            if (state.connectionMode == ConnectionMode.BLUETOOTH && state.role == AppRole.CONTROLLER) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("BLUETOOTH DEVICES (ZERO WI-FI)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhitePrimary, letterSpacing = 1.sp)
                            }

                            IconButton(
                                onClick = onScanBluetooth,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Status: ${state.bluetoothStatus}",
                            fontSize = 11.5.sp,
                            color = if (state.isLiveConnected) DotGreenMild else GraySecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.bluetoothDevices.isEmpty()) {
                            Text(
                                text = "No paired Bluetooth devices found.\n\n💡 How to connect via Bluetooth:\n1. Pair your phone with your Windows laptop in Android Settings > Bluetooth.\n2. Tap the Refresh icon above.\n3. Tap Connect beside your laptop name.",
                                fontSize = 12.sp,
                                color = GraySecondary,
                                lineHeight = 17.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.bluetoothDevices.forEach { device ->
                                    val isConnectedToThis = state.isLiveConnected && (state.selectedBluetoothDevice?.address == device.address || state.activeHostName.contains(device.name, ignoreCase = true))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isConnectedToThis) ElevatedSurface else DarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnectedToThis) DotGreenMild else BorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WhitePrimary)
                                                Text(device.address, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = GrayMuted)
                                            }

                                            if (isConnectedToThis) {
                                                OutlinedButton(
                                                    onClick = onDisconnectBluetooth,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Disconnect", fontSize = 11.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { onConnectBluetooth(device) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Discovered Laptops (Auto-Discovery)
            if (state.role == AppRole.CONTROLLER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCOVERED LAPTOPS (AUTO-DISCOVERY)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DotGreenMild,
                        letterSpacing = 1.2.sp
                    )

                    OutlinedButton(
                        onClick = onRefreshHardware,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Scan Wi-Fi", fontSize = 10.sp, color = WhitePrimary)
                    }
                }

                if (state.discoveredHosts.isEmpty()) {
                    Surface(
                        color = CardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Scanning Wi-Fi for AirGlide Laptop Receivers...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhitePrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Make sure AirGlide is running on your laptop on the same Wi-Fi.",
                                    fontSize = 11.sp,
                                    color = GrayMuted
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.discoveredHosts.forEach { host ->
                            val isCurrent = host.ipAddress == state.settings.targetHostIp && host.port == state.settings.targetPort
                            Surface(
                                color = if (isCurrent) ElevatedSurface else CardDark,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isCurrent) DotGreenMild else BorderDark
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Laptop,
                                            contentDescription = "Laptop",
                                            tint = if (isCurrent) DotGreenMild else WhitePrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = host.hostName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = WhitePrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(DotGreenMild)
                                                )
                                            }
                                            Text(
                                                text = "${host.ipAddress}:${host.port}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = GraySecondary
                                            )
                                        }
                                    }

                                    if (isCurrent) {
                                        Button(
                                            onClick = onDisconnect,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2D1414),
                                                contentColor = Color(0xFFF87171)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "Disconnect",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF87171)
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                inputIp = host.ipAddress
                                                inputPort = host.port.toString()
                                                onSelectDiscoveredHost(host)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = WhitePrimary,
                                                contentColor = TrueBlack
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "Connect",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrueBlack
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Wi-Fi & Bluetooth Diagnostics & Turn-On Suggestions
            if (!state.isWifiEnabled) {
                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DotAmberMild),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Wi-Fi Off",
                                tint = DotAmberMild,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Wi-Fi is Turned Off",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhitePrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Turn on Wi-Fi or enable a Mobile Hotspot so your phone and laptop can pair on the same local network.",
                                    fontSize = 12.sp,
                                    color = GraySecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhitePrimary,
                                contentColor = TrueBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn on Wi-Fi in Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (!state.isBluetoothEnabled) {
                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothDisabled,
                                contentDescription = "Bluetooth Off",
                                tint = GrayMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bluetooth is Turned Off",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhitePrimary
                                )
                                Text(
                                    text = "Optional: enable if using direct Bluetooth mode",
                                    fontSize = 11.sp,
                                    color = GrayMuted
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (_: Exception) {}
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Turn on BT", fontSize = 11.sp, color = WhitePrimary)
                        }
                    }
                }
            }

            // Target Workstation Connection Box
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANUAL IP CONNECTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputIp,
                            onValueChange = { inputIp = it },
                            label = { Text("Laptop IP Address") },
                            placeholder = { Text("e.g. 192.168.1.105") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhitePrimary,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = WhitePrimary,
                                unfocusedTextColor = WhitePrimary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("connectivity_ip_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val suggested = NetworkUtils.suggestSubnetHostIp(state.localIp)
                                inputIp = suggested
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Suggest Subnet IP", tint = WhitePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputPort,
                            onValueChange = { inputPort = it },
                            label = { Text("Port") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhitePrimary,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = WhitePrimary,
                                unfocusedTextColor = WhitePrimary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.4f)
                                .testTag("connectivity_port_input")
                        )

                        Button(
                            onClick = {
                                val p = inputPort.toIntOrNull() ?: 5055
                                onConnectToIp(inputIp, p)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhitePrimary,
                                contentColor = TrueBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.6f)
                                .height(54.dp)
                                .testTag("apply_connection_button")
                        ) {
                            Text("Set & Connect", fontWeight = FontWeight.Bold, color = TrueBlack)
                        }
                    }
                }
            }

            // History of Connected Devices
            Text(
                text = "HISTORY OF CONNECTED DEVICES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.2.sp
            )

            if (state.deviceHistory.isEmpty()) {
                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No previous connection history yet",
                            fontSize = 12.sp,
                            color = GrayMuted
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.deviceHistory.forEach { item ->
                        val isCurrent = item.ipAddress == state.settings.targetHostIp && item.port == state.settings.targetPort
                        Surface(
                            color = if (isCurrent) ElevatedSurface else CardDark,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) WhitePrimary else BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Laptop,
                                        contentDescription = "Device",
                                        tint = if (isCurrent) WhitePrimary else GrayMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = WhitePrimary
                                        )
                                        Text(
                                            text = "${item.ipAddress}:${item.port}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = GraySecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            inputIp = item.ipAddress
                                            inputPort = item.port.toString()
                                            onSelectHistoryDevice(item)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrent) WhitePrimary else ElevatedSurface,
                                            contentColor = if (isCurrent) TrueBlack else WhitePrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrent) "Active" else "Connect",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteHistoryItem(item) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = GrayMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Available Devices (When running on Laptop / Tablet Host screen)
            if (state.role == AppRole.HOST) {
                Text(
                    text = "AVAILABLE CONTROLLER DEVICES (HOST MODE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    letterSpacing = 1.2.sp
                )

                if (state.hostDevices.isEmpty()) {
                    Surface(
                        color = CardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Listening on port ${state.settings.targetPort}... No mobile controllers discovered yet.",
                                fontSize = 12.sp,
                                color = GrayMuted
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.hostDevices.forEach { dev ->
                            val isSelected = dev.deviceId == state.hostActiveDeviceId
                            Surface(
                                color = if (isSelected) ElevatedSurface else CardDark,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) WhitePrimary else BorderDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectHostActiveDevice(dev.deviceId) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectHostActiveDevice(dev.deviceId) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = WhitePrimary,
                                                unselectedColor = GrayMuted
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = dev.deviceName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = WhitePrimary
                                            )
                                            Text(
                                                text = "ID: ${dev.deviceId} | IP: ${dev.ipAddress}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = GraySecondary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${dev.packetsReceived} pkts",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (isSelected) WhitePrimary else GrayMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Desktop Server Helper Button
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenCompanionScript() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Script",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Laptop Companion Server Script",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "View & copy python script (Windows/Mac/Linux)",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenCompanionScript,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Code", fontSize = 11.sp, color = WhitePrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

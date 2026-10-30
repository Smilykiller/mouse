package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.model.BluetoothDeviceInfo
import com.example.model.ConnectionMode
import com.example.model.DiscoveredHost
import com.example.model.TouchpadLayout
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
fun HubSettingsScreen(
    state: AirGlideUiState,
    onNavigate: (AppScreen) -> Unit,
    onUpdateSensitivity: (Float) -> Unit,
    onUpdateScrollSensitivity: (Float) -> Unit,
    onToggleInvertScroll: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleAirMotion: () -> Unit,
    onCalibrateAirMotion: () -> Unit,
    onUpdateAirMotionSensitivity: (Float) -> Unit,
    onToggleVolumeButtons: () -> Unit,
    onConnectToDiscoveredHost: (DiscoveredHost) -> Unit,
    onDisconnect: () -> Unit = {},
    onLayoutChange: (TouchpadLayout) -> Unit,
    onRoleChange: (AppRole) -> Unit,
    onSelectConnectionMode: (ConnectionMode) -> Unit = {},
    onScanBluetooth: () -> Unit = {},
    onConnectBluetooth: (BluetoothDeviceInfo) -> Unit = {},
    onDisconnectBluetooth: () -> Unit = {},
    onOpenMousepad: () -> Unit = {},
    onOpenInstallOnLaptop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Launch CTA
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AirGlide",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Ultra-Low Latency Wireless Remote",
                        fontSize = 12.sp,
                        color = GraySecondary
                    )
                }

                // Quick role badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onRoleChange(if (state.role == AppRole.CONTROLLER) AppRole.HOST else AppRole.CONTROLLER)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.role == AppRole.CONTROLLER) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                            contentDescription = "Role",
                            tint = WhitePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.role == AppRole.CONTROLLER) "Mobile Controller" else "Host Receiver",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhitePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Launch Controller Button
            Button(
                onClick = { onOpenMousepad() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("launch_touchpad_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mouse,
                        contentDescription = "Launch",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enter Full-Screen Mousepad",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Connection Mode Switcher (Wi-Fi vs Bluetooth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.connectionMode == ConnectionMode.WIFI_UDP) Color(0xFF131A26) else CardDark,
                    border = BorderStroke(
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
                            Text("Wi-Fi / LAN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                            Text("Fast & auto-find", fontSize = 10.sp, color = GraySecondary)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.connectionMode == ConnectionMode.BLUETOOTH) Color(0xFF131A26) else CardDark,
                    border = BorderStroke(
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
                            Text("Bluetooth", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                            Text("Offline & travel", fontSize = 10.sp, color = GraySecondary)
                        }
                    }
                }
            }
        }

        // Bluetooth Devices List (Shown when Bluetooth Mode is active)
        if (state.connectionMode == ConnectionMode.BLUETOOTH && state.role == AppRole.CONTROLLER) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardDark,
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
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
                                val isConnectedToThis = state.isLiveConnected && state.selectedBluetoothDevice?.address == device.address
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isConnectedToThis) ElevatedSurface else DarkSurface,
                                    border = BorderStroke(1.dp, if (isConnectedToThis) DotGreenMild else BorderDark),
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
                                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
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

        // Section: Discovered Laptops (Auto-Discovery Banner)
        if (state.discoveredHosts.isNotEmpty() && state.role == AppRole.CONTROLLER) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DISCOVERED LAPTOPS ON YOUR WI-FI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DotGreenMild,
                    letterSpacing = 1.2.sp
                )

                state.discoveredHosts.forEach { host ->
                    val isCurrent = host.ipAddress == state.settings.targetHostIp && host.port == state.settings.targetPort
                    Surface(
                        color = if (isCurrent) ElevatedSurface else CardDark,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isCurrent) DotGreenMild else BorderHighlight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSurface,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Laptop,
                                            contentDescription = "Laptop",
                                            tint = if (isCurrent) DotGreenMild else WhitePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
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
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
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
                                    onClick = { onConnectToDiscoveredHost(host) },
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

        // Section: Device Connectivity Manage Card
        Text(
            text = "DEVICE CONNECTIVITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayMuted,
            letterSpacing = 1.2.sp
        )

        Surface(
            color = CardDark,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onNavigate(AppScreen.DEVICE_CONNECTION) }
                .testTag("device_connectivity_nav_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElevatedSurface,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "Connectivity",
                                tint = WhitePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Device Connection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            state.isLiveConnected -> DotGreenMild
                                            state.isLocalNetworkActive -> DotAmberMild
                                            else -> DotRedMild
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.role == AppRole.CONTROLLER) {
                                if (state.isLiveConnected) "Live: ${state.activeHostName} (${state.settings.targetHostIp})"
                                else if (state.isLocalNetworkActive) "Target: ${state.settings.targetHostIp}:${state.settings.targetPort}"
                                else "Not Connected (Wi-Fi / Hotspot needed)"
                            } else {
                                "${state.hostDevices.size} available controllers"
                            },
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GraySecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Manage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GraySecondary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = GraySecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Install Receiver on Laptop Card
        Surface(
            color = Color(0xFF131A26),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenInstallOnLaptop() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = "Laptop",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Install Receiver on Laptop",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Beam via Wi-Fi drop or Bluetooth",
                            fontSize = 12.sp,
                            color = GraySecondary
                        )
                    }
                }

                Button(
                    onClick = onOpenInstallOnLaptop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Speed & Sensitivity Settings
        Text(
            text = "SPEED & SENSITIVITY CALIBRATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayMuted,
            letterSpacing = 1.2.sp
        )

        // Accelerometer-based Cursor Control (Tilt-and-Move)
        Surface(
            color = CardDark,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Air Motion",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gyroscope Air-Mouse",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Wave or tilt phone to move cursor in air",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }

                    Switch(
                        checked = state.isAirMotionActive,
                        onCheckedChange = { onToggleAirMotion() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrueBlack,
                            checkedTrackColor = WhitePrimary,
                            uncheckedThumbColor = GraySecondary,
                            uncheckedTrackColor = DarkSurface
                        ),
                        modifier = Modifier.testTag("air_motion_switch")
                    )
                }

                if (state.isAirMotionActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tilt Motion Sensitivity",
                            fontSize = 12.sp,
                            color = GraySecondary
                        )
                        Text(
                            text = "${"%.1f".format(state.airMotionSensitivity)}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary
                        )
                    }
                    Slider(
                        value = state.airMotionSensitivity,
                        onValueChange = onUpdateAirMotionSensitivity,
                        valueRange = 1.0f..6.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = WhitePrimary,
                            activeTrackColor = WhitePrimary,
                            inactiveTrackColor = BorderDark
                        ),
                        modifier = Modifier.testTag("air_motion_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = onCalibrateAirMotion,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ElevatedSurface,
                            contentColor = WhitePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recenter / Calibrate Neutral Angle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Touchpad Swipe Speed
        Surface(
            color = CardDark,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Touchpad Speed",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Touchpad Swipe Speed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhitePrimary
                        )
                    }
                    Text(
                        text = "${"%.1f".format(state.settings.sensitivity)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = state.settings.sensitivity,
                    onValueChange = onUpdateSensitivity,
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = WhitePrimary,
                        activeTrackColor = WhitePrimary,
                        inactiveTrackColor = BorderDark
                    ),
                    modifier = Modifier.testTag("touchpad_sensitivity_slider")
                )
            }
        }

        // Scroll Speed & Direction
        Surface(
            color = CardDark,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Scroll Speed",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scroll Wheel Speed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhitePrimary
                        )
                    }
                    Text(
                        text = "${"%.1f".format(state.settings.scrollSensitivity)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = state.settings.scrollSensitivity,
                    onValueChange = onUpdateScrollSensitivity,
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = WhitePrimary,
                        activeTrackColor = WhitePrimary,
                        inactiveTrackColor = BorderDark
                    ),
                    modifier = Modifier.testTag("scroll_sensitivity_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invert Scroll Direction (Natural)",
                        fontSize = 13.sp,
                        color = GraySecondary
                    )
                    Switch(
                        checked = state.settings.invertScroll,
                        onCheckedChange = { onToggleInvertScroll() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrueBlack,
                            checkedTrackColor = WhitePrimary
                        )
                    )
                }
            }
        }

        // Section: Hardware Inputs & Feedback
        Text(
            text = "HARDWARE BUTTONS & HAPTICS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayMuted,
            letterSpacing = 1.2.sp
        )

        Surface(
            color = CardDark,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Volume keys Before / After
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume Keys",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Volume Keys as Before / After",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Vol Up = Before (Page Up) • Vol Down = After (Page Down)",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }
                    Switch(
                        checked = state.useVolumeButtonsForNavigation,
                        onCheckedChange = { onToggleVolumeButtons() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrueBlack,
                            checkedTrackColor = WhitePrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Haptic feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Haptics",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Haptic Tactile Clicks",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Tactile mechanical vibration on clicks & scroll",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }
                    Switch(
                        checked = state.settings.hapticFeedback,
                        onCheckedChange = { onToggleHaptics() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TrueBlack,
                            checkedTrackColor = WhitePrimary
                        )
                    )
                }
            }
        }

        // Section: Touchpad Layout Selection
        Text(
            text = "TOUCHPAD CONTROL LAYOUT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GrayMuted,
            letterSpacing = 1.2.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TouchpadLayout.values().forEach { layoutOption ->
                val isSelected = state.layout == layoutOption
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ElevatedSurface else CardDark,
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isSelected) WhitePrimary else BorderDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLayoutChange(layoutOption) }
                        .testTag("layout_option_${layoutOption.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layoutOption.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = layoutOption.description,
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = WhitePrimary,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(TrueBlack)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

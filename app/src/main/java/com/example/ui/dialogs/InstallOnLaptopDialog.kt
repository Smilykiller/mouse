package com.example.ui.dialogs

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BluetoothDeviceInfo
import com.example.model.DiscoveredHost
import com.example.model.InstallTargetType
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState

@Composable
fun InstallOnLaptopDialog(
    state: AirGlideUiState,
    onSelectTargetType: (InstallTargetType) -> Unit = {},
    onSelectBluetoothDevice: (BluetoothDeviceInfo) -> Unit = {},
    onSelectWifiHost: (DiscoveredHost?) -> Unit = {},
    onSendInstallRequest: (Context) -> Unit = {},
    onStartWifiDrop: (Context) -> Unit = {},
    onStopWifiDrop: () -> Unit = {},
    onBeamBluetooth: (Context) -> Unit = {},
    onSaveToDownloads: (Context) -> Unit = {},
    onShareGeneric: (Context) -> Unit = {},
    onScanBluetooth: () -> Unit = {},
    onOpenTrackpad: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Auto-start Wi-Fi server when Wi-Fi tab is active
    LaunchedEffect(state.installTargetType) {
        if (state.installTargetType == InstallTargetType.WIFI && !state.isWifiDropServerRunning) {
            onStartWifiDrop(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0C0F17),
            border = BorderStroke(1.dp, Color(0xFF1E2638))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF06B6D4).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Install on Laptop",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Select device • Send request • 1-Click install",
                                fontSize = 11.5.sp,
                                color = GraySecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GraySecondary
                        )
                    }
                }

                // ==========================================================
                // Channel Selector: [📡 Bluetooth Beam] | [📶 Wi-Fi Drop]
                // ==========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isBt = state.installTargetType == InstallTargetType.BLUETOOTH
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isBt) Color(0xFF0369A1).copy(alpha = 0.25f) else CardDark,
                        border = BorderStroke(1.dp, if (isBt) Color(0xFF0284C7) else BorderDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTargetType(InstallTargetType.BLUETOOTH) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isBt) Color(0xFF38BDF8) else GrayMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bluetooth (Zero Wi-Fi)",
                                fontSize = 11.5.sp,
                                fontWeight = if (isBt) FontWeight.Bold else FontWeight.Normal,
                                color = if (isBt) WhitePrimary else GraySecondary
                            )
                        }
                    }

                    val isWifi = state.installTargetType == InstallTargetType.WIFI
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isWifi) Color(0xFF0369A1).copy(alpha = 0.25f) else CardDark,
                        border = BorderStroke(1.dp, if (isWifi) Color(0xFF0284C7) else BorderDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTargetType(InstallTargetType.WIFI) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isWifi) Color(0xFF38BDF8) else GrayMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wi-Fi Network Drop",
                                fontSize = 11.5.sp,
                                fontWeight = if (isWifi) FontWeight.Bold else FontWeight.Normal,
                                color = if (isWifi) WhitePrimary else GraySecondary
                            )
                        }
                    }
                }

                // ==========================================================
                // Step 1: List and Select Target Device
                // ==========================================================
                Text(
                    text = if (state.installTargetType == InstallTargetType.BLUETOOTH)
                        "1. SELECT TARGET LAPTOP (BLUETOOTH)"
                    else
                        "1. SELECT TARGET LAPTOP (WI-FI)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.6.sp
                )

                if (state.installTargetType == InstallTargetType.BLUETOOTH) {
                    // --- Bluetooth Devices List ---
                    if (state.bluetoothDevices.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No paired Bluetooth computers found.",
                                    fontSize = 12.sp,
                                    color = GraySecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Make sure your laptop is paired with your phone in Windows Settings > Bluetooth.",
                                    fontSize = 11.sp,
                                    color = GrayMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onScanBluetooth,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh Bluetooth Devices", fontSize = 11.5.sp)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.bluetoothDevices.forEach { dev ->
                                val isSelected = state.selectedInstallBluetoothDevice?.address == dev.address
                                val isLive = state.activeHostName.equals(dev.name, ignoreCase = true) && state.isLiveConnected

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.16f) else CardDark,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else BorderDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectBluetoothDevice(dev) }
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
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else DarkSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (dev.isComputer) Icons.Default.Laptop else Icons.Default.Bluetooth,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color(0xFF38BDF8) else GrayMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = dev.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = WhitePrimary
                                                )
                                                Text(
                                                    text = dev.address,
                                                    fontSize = 10.5.sp,
                                                    color = GrayMuted
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isLive) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(DotGreenMild.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("● LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DotGreenMild)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            } else if (dev.isComputer) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("PC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            // Radio selection indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color(0xFF38BDF8) else DarkSurface)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF0C0F17),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- Wi-Fi Target List ---
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Option 1: Discovered AirGlide hosts (if any)
                        state.discoveredHosts.forEach { host ->
                            val isSelected = state.selectedInstallWifiHost?.ipAddress == host.ipAddress
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.16f) else CardDark,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else BorderDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectWifiHost(host) }
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
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Laptop, contentDescription = null, tint = DotGreenMild, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(host.hostName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                                            Text("${host.ipAddress}:${host.port}", fontSize = 10.5.sp, color = GrayMuted)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(DotGreenMild.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("READY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DotGreenMild)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF38BDF8) else DarkSurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF0C0F17), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Option 2: General Local Wi-Fi Web Drop (Direct Phone-to-Laptop HTTP link)
                        val isDirectDropSelected = state.selectedInstallWifiHost == null
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDirectDropSelected) Color(0xFF0284C7).copy(alpha = 0.16f) else CardDark,
                            border = BorderStroke(
                                if (isDirectDropSelected) 1.5.dp else 1.dp,
                                if (isDirectDropSelected) Color(0xFF38BDF8) else BorderDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectWifiHost(null) }
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
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Any Laptop on Wi-Fi (Web Drop)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                                        Text(
                                            if (state.wifiDropServerUrl.isNotBlank()) state.wifiDropServerUrl else "http://${state.localIp}:8080",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("50 MB/s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isDirectDropSelected) Color(0xFF38BDF8) else DarkSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDirectDropSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF0C0F17), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================================
                // Step 2: Target Card & Send Install Request Button
                // ==========================================================
                val targetName = if (state.installTargetType == InstallTargetType.BLUETOOTH) {
                    state.selectedInstallBluetoothDevice?.name ?: "Paired Laptop"
                } else {
                    state.selectedInstallWifiHost?.hostName ?: "Local Laptop"
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, Color(0xFF1E2638)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. SEND INSTALLATION REQUEST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "v2.1.0 Self-Installer",
                                fontSize = 10.5.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (state.installTargetType == InstallTargetType.BLUETOOTH)
                                "Beams AirGlide_Laptop_Receiver.exe directly to '$targetName' over Bluetooth OPP. Once accepted, it installs shortcuts and registers in Windows Settings."
                            else
                                "Streams AirGlide_Laptop_Receiver.exe directly to '$targetName' at full router speed (zero internet needed).",
                            fontSize = 11.5.sp,
                            color = GraySecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onSendInstallRequest(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (state.installTargetType == InstallTargetType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🚀 Send Install Request to $targetName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ==========================================================
                // Step 3: Live Acceptance & Connection Monitor
                // ==========================================================
                if (state.isInstallRequestSent || state.wifiDropDownloadCount > 0 || state.isLiveConnected) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(
                            1.dp,
                            if (state.isLiveConnected) DotGreenMild else Color(0xFF0284C7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (state.isLiveConnected) {
                                // --- Connected & Active State ---
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(DotGreenMild.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = DotGreenMild, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🎉 Installed & Linked to $targetName!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DotGreenMild
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AirGlide desktop receiver is active and running on your laptop. You can now use your phone as a full-screen trackpad, keyboard, and stream deck!",
                                    fontSize = 11.5.sp,
                                    color = GraySecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = onOpenTrackpad,
                                    colors = ButtonDefaults.buttonColors(containerColor = DotGreenMild),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "Open Trackpad ➔",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF070B14)
                                    )
                                }
                            } else if (state.wifiDropDownloadCount > 0) {
                                // --- Downloaded / Accepted State ---
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "📥 Accepted by Laptop! (Download Complete)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Launch AirGlide_Laptop_Receiver.exe from your laptop's Downloads folder. Once opened, it will register Start Menu shortcuts and connect automatically!",
                                    fontSize = 11.5.sp,
                                    color = GraySecondary
                                )
                            } else {
                                // --- Waiting for Acceptance State ---
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38BDF8))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Waiting for acceptance on $targetName...",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhitePrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (state.installTargetType == InstallTargetType.BLUETOOTH) {
                                    Text(
                                        text = "👉 Check your Windows laptop screen:\nWindows will display a balloon notification: 'Bluetooth File Transfer: Incoming file from ${state.activeHostName}...'. Click 'Accept' or 'Save' to receive the installer.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 16.sp
                                    )
                                } else {
                                    Text(
                                        text = "👉 Open Chrome or Edge on your laptop and go to:\n${state.wifiDropServerUrl.ifBlank { "http://" + state.localIp + ":8080" }}\nThen click 'Accept & Download'.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val url = state.wifiDropServerUrl.ifBlank { "http://${state.localIp}:8080" }
                                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cb.setPrimaryClip(ClipData.newPlainText("AirGlide Download Link", url))
                                                Toast.makeText(context, "Copied link to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, BorderDark),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Link", fontSize = 11.sp, color = WhitePrimary)
                                        }

                                        Button(
                                            onClick = { onShareGeneric(context) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share Link", fontSize = 11.sp, color = WhitePrimary)
                                        }
                                    }
                                }
                            }

                            state.wifiDropStatus?.let { status ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = status,
                                    fontSize = 10.5.sp,
                                    color = if (status.startsWith("✓")) DotGreenMild else GraySecondary
                                )
                            }
                        }
                    }
                }

                // ==========================================================
                // Fallback: USB Cable Option
                // ==========================================================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("USB Cable / Local File Copy", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                                    Text("Saves .exe & .zip to phone Downloads (visible in Files & USB)", fontSize = 10.5.sp, color = GrayMuted)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (state.wifiDropStatus?.contains("Saved to Downloads") == true) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Opened phone Downloads folder", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("📂 View Files", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onSaveToDownloads(context) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        if (state.wifiDropStatus?.contains("Saved to Downloads") == true) "Save Again" else "Save to Phone",
                                        fontSize = 10.5.sp,
                                        color = WhitePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Uninstall & Security Reassurance
                Text(
                    text = "💡 Clean & Instant Uninstall: AirGlide automatically registers in Windows Settings > Installed Apps. You can remove it anytime in 1 click from Windows Settings, app Settings, or via Uninstall_AirGlide_Windows.bat.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 14.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

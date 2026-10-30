package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.runtime.DisposableEffect
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
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState

@Composable
fun InstallOnLaptopDialog(
    state: AirGlideUiState,
    onStartWifiDrop: (Context) -> Unit,
    onStopWifiDrop: () -> Unit,
    onBeamBluetooth: (Context) -> Unit,
    onSaveToDownloads: (Context) -> Unit,
    onShareGeneric: (Context) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Auto-start Wi-Fi server when dialog opens if on local network
    LaunchedEffect(Unit) {
        if (!state.isWifiDropServerRunning) {
            onStartWifiDrop(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F1219),
            border = BorderStroke(1.dp, Color(0xFF1E2638))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header
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
                                text = "Zero web download • Direct from phone",
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
                // Option 1: Wi-Fi Local Web Drop
                // ==========================================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, if (state.isWifiDropServerRunning) Color(0xFF0284C7) else BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("METHOD 1: WI-FI LOCAL DROP (FASTEST)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                            }

                            if (state.isWifiDropServerRunning) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("● LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DotGreenMild)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open Chrome or Edge on your laptop and go to the link below to download directly from your phone:",
                            fontSize = 11.5.sp,
                            color = GraySecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // URL Pill & Copy Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.isWifiDropServerRunning && state.wifiDropServerUrl.isNotBlank())
                                        state.wifiDropServerUrl
                                    else
                                        "http://${state.localIp}:8080",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val url = state.wifiDropServerUrl.ifBlank { "http://${state.localIp}:8080" }
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("AirGlide Download Link", url))
                                            Toast.makeText(context, "Copied link to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = WhitePrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp, color = WhitePrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (state.wifiDropDownloadCount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✓ Downloaded to laptop ${state.wifiDropDownloadCount} time(s)!",
                                fontSize = 11.sp,
                                color = DotGreenMild,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ==========================================================
                // Option 2: Bluetooth Direct Beam
                // ==========================================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("METHOD 2: BLUETOOTH BEAM (ZERO WI-FI)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Beams AirGlide_Laptop_Receiver.exe directly to your paired laptop using Bluetooth OPP. Laptop will ask: 'Save file'.",
                            fontSize = 11.5.sp,
                            color = GraySecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onBeamBluetooth(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Beam Receiver via Bluetooth", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================================
                // Option 3: Phone Storage & Quick Share
                // ==========================================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("METHOD 3: USB CABLE / QUICK SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Plug phone into laptop with charging cable: file will appear in laptop File Explorer under Downloads/AirGlide.",
                            fontSize = 11.5.sp,
                            color = GraySecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSaveToDownloads(context) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("Save to Phone", fontSize = 11.5.sp, color = WhitePrimary, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onShareGeneric(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Share", fontSize = 11.5.sp, color = WhitePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                state.wifiDropStatus?.let { status ->
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        color = if (status.startsWith("✓")) DotGreenMild else GraySecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

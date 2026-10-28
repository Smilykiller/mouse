package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary

@Composable
fun PermissionsAndSecurityDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = WhitePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Permissions & Security",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "100% Unidirectional • Air-Gapped Local Control",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GrayMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Unidirectional Guarantee Banner
                Surface(
                    color = TrueBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield Lock",
                            tint = WhitePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UNIDIRECTIONAL AIR-GAP PROTOCOL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Data flows in ONE direction only: Mobile → Laptop. The app never requests or accepts return data, screen captures, or workstation files, ensuring zero external access risk to your laptop.",
                                fontSize = 12.sp,
                                color = GraySecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "REQUIRED AND OPTIONAL PERMISSIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrayMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionItem(
                    icon = Icons.Default.Wifi,
                    name = "Local Network (Wi-Fi & Hotspot)",
                    type = "Install-time (Normal)",
                    desc = "Sends UDP packets across the same local subnet or mobile hotspot. No internet access or router required."
                )

                PermissionItem(
                    icon = Icons.Default.CompassCalibration,
                    name = "Accelerometer / Motion Sensor",
                    type = "Sensor (No special permission)",
                    desc = "Used for Air-Mouse mode so you can tilt and wave the phone in the air to move the cursor."
                )

                PermissionItem(
                    icon = Icons.Default.VolumeUp,
                    name = "Physical Volume Keys",
                    type = "Hardware Key Intercept",
                    desc = "Volume Up/Down keys trigger Before/After, Page Up/Down, or Slide navigation without screen taps."
                )

                PermissionItem(
                    icon = Icons.Default.Vibration,
                    name = "Haptic Tactile Engine",
                    type = "Install-time (VIBRATE)",
                    desc = "Generates crisp haptic clicks to mimic physical trackpad and mouse button switches."
                )

                PermissionItem(
                    icon = Icons.Default.Bluetooth,
                    name = "Bluetooth (Optional)",
                    type = "Runtime (BLUETOOTH_CONNECT)",
                    desc = "Used only when switching from Wi-Fi to direct Bluetooth wireless mode."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhitePrimary,
                        contentColor = TrueBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = TrueBlack)
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    name: String,
    type: String,
    desc: String
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = WhitePrimary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = WhitePrimary
                    )
                    Text(
                        text = type,
                        fontSize = 10.sp,
                        color = GrayMuted
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = GraySecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

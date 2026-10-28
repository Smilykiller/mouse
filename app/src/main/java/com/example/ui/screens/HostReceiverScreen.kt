package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectedDevice
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotAmberMild
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState

@Composable
fun HostReceiverScreen(
    state: AirGlideUiState,
    onSelectActiveDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Column: Connected Devices & Arbitration Panel
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
        ) {
            // Header Card
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Arbitration",
                                tint = WhitePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Arbitration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                text = "PORT ${state.settings.targetPort}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select one active controller. All other devices are blocked to eliminate accidental conflicts.",
                        fontSize = 12.sp,
                        color = GraySecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Devices List
            if (state.hostDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Waiting for connections",
                            tint = GrayMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Waiting for Mobile Controllers...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhitePrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connect phone to ${state.localIp}:${state.settings.targetPort}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GraySecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.hostDevices) { device ->
                        DeviceArbitrationItem(
                            device = device,
                            isActive = device.deviceId == state.hostActiveDeviceId,
                            onSelect = { onSelectActiveDevice(device.deviceId) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Packet Console Log
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "REAL-TIME UNIDIRECTIONAL STREAM LOG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(state.hostPacketLogs) { logEntry ->
                            Text(
                                text = logEntry,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (logEntry.contains("BLOCKED")) DotAmberMild else WhitePrimary
                            )
                        }
                    }
                }
            }
        }

        // Right Column: Live Simulated Workspace / Cursor Display
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
        ) {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("host_cursor_canvas_container")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Grid lines
                        val step = 36.dp.toPx()
                        var x = 0f
                        while (x < canvasW) {
                            drawLine(
                                color = BorderDark.copy(alpha = 0.35f),
                                start = Offset(x, 0f),
                                end = Offset(x, canvasH)
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < canvasH) {
                            drawLine(
                                color = BorderDark.copy(alpha = 0.35f),
                                start = Offset(0f, y),
                                end = Offset(canvasW, y)
                            )
                            y += step
                        }

                        // Map virtual cursor (0-1920, 0-1080) to canvas dimensions
                        val curX = (state.hostCursorPosition.x / 1920f) * canvasW
                        val curY = (state.hostCursorPosition.y / 1080f) * canvasH

                        val cursorRadius = 12.dp.toPx()
                        drawCircle(
                            color = WhitePrimary.copy(alpha = 0.2f),
                            radius = cursorRadius * 2.2f,
                            center = Offset(curX, curY)
                        )
                        drawCircle(
                            color = WhitePrimary,
                            radius = cursorRadius,
                            center = Offset(curX, curY)
                        )
                    }

                    // Floating status bar inside canvas
                    Surface(
                        color = DarkSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cursor: (${state.hostCursorPosition.x.toInt()}, ${state.hostCursorPosition.y.toInt()})",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = WhitePrimary
                            )
                            state.hostClickFlash?.let { click ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = WhitePrimary
                                ) {
                                    Text(
                                        text = click.label.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TrueBlack,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Canvas Footer
                    Text(
                        text = "LIVE HOST DISPLAY (ACTIVE CONTROLLER STREAM)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceArbitrationItem(
    device: ConnectedDevice,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isActive) WhitePrimary else BorderDark
    val bgColor = if (isActive) ElevatedSurface else CardDark

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("device_item_${device.deviceId}")
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
                RadioButton(
                    selected = isActive,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WhitePrimary,
                        unselectedColor = GrayMuted
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.deviceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = WhitePrimary
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElevatedSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhitePrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "ID: ${device.deviceId} | IP: ${device.ipAddress}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GraySecondary
                    )
                }
            }

            Text(
                text = "${device.packetsReceived} pkts",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isActive) WhitePrimary else GrayMuted
            )
        }
    }
}

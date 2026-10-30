package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun DeckScreen(
    state: AirGlideUiState,
    onTriggerAction: (String) -> Unit,
    onJumpDisplay: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    onSetAutoScrollSpeed: (Float) -> Unit,
    onStopAutoScroll: () -> Unit,
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
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = Color(0xFFA78BFA),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Deck & Productivity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePrimary
                )
                Text(
                    text = "Stream deck, browser wheel & auto-scroller",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
            }
        }

        // ==============================================================
        // 1. MULTI-MONITOR DISPLAY JUMP
        // ==============================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderDark),
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Multi-Monitor Quick Jump",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WhitePrimary
                        )
                        Text(
                            text = "Teleports cursor between displays",
                            fontSize = 11.sp,
                            color = GraySecondary
                        )
                    }
                }

                Button(
                    onClick = onJumpDisplay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Snap Display", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==============================================================
        // 2. BROWSER TAB ROTARY WHEEL
        // ==============================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tab, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BROWSER TAB WHEEL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayMuted, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Slide across the wheel or tap buttons to flick through open browser tabs.",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Rotary Touch Surface
                var dragAccumulator by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount.x
                                if (dragAccumulator > 38f) {
                                    onSwitchTab(1) // Next tab (Ctrl + Tab)
                                    dragAccumulator = 0f
                                } else if (dragAccumulator < -38f) {
                                    onSwitchTab(-1) // Prev tab (Ctrl + Shift + Tab)
                                    dragAccumulator = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSwitchTab(-1) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Prev Tab", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev Tab", fontSize = 11.5.sp)
                        }

                        Text(
                            text = "↔ SWIPE OR SPIN ↔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = { onSwitchTab(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Next Tab", fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Tab", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ==============================================================
        // 3. TELEPROMPTER & SMOOTH AUTO-SCROLLER
        // ==============================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = DotGreenMild, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TELEPROMPTER AUTO-SCROLLER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayMuted, letterSpacing = 1.sp)
                    }

                    if (state.isAutoScrolling) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DotGreenMild.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, DotGreenMild)
                        ) {
                            Text(
                                text = "ACTIVE (${state.autoScrollSpeed.toInt()}x)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DotGreenMild,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hands-free continuous scrolling for reading long documents, articles, code, or speech scripts.",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSetAutoScrollSpeed(1.5f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.autoScrollSpeed == 1.5f) Color(0xFF0284C7) else DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Slow (1x)", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onSetAutoScrollSpeed(3.0f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.autoScrollSpeed == 3.0f) Color(0xFF0284C7) else DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Medium (2x)", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onSetAutoScrollSpeed(5.0f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.autoScrollSpeed == 5.0f) Color(0xFF0284C7) else DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fast (3x)", fontSize = 11.sp)
                    }

                    if (state.isAutoScrolling) {
                        Button(
                            onClick = onStopAutoScroll,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ==============================================================
        // 4. STREAM DECK GRID
        // ==============================================================
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("STREAM DECK & SHORTCUTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayMuted, letterSpacing = 1.sp)
        }

        // Category A: Quick Apps
        DeckSectionHeader("🚀 FAST APP LAUNCHERS")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckActionButton("Chrome", Icons.Default.Language, Color(0xFF38BDF8), Modifier.weight(1f)) { onTriggerAction("CHROME") }
            DeckActionButton("VS Code", Icons.Default.Code, Color(0xFF60A5FA), Modifier.weight(1f)) { onTriggerAction("VSCODE") }
            DeckActionButton("Spotify", Icons.Default.MusicNote, Color(0xFF34D399), Modifier.weight(1f)) { onTriggerAction("SPOTIFY") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckActionButton("Terminal", Icons.Default.Terminal, Color(0xFFFBBF24), Modifier.weight(1f)) { onTriggerAction("TERMINAL") }
            DeckActionButton("Explorer", Icons.Default.Folder, Color(0xFFF59E0B), Modifier.weight(1f)) { onTriggerAction("EXPLORER") }
            DeckActionButton("Task Mgr", Icons.Default.Speed, Color(0xFFEC4899), Modifier.weight(1f)) { onTriggerAction("TASKMGR") }
        }

        // Category B: System & Window Controls
        Spacer(modifier = Modifier.height(4.dp))
        DeckSectionHeader("⚡ WINDOWS POWER & PRODUCTIVITY")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckActionButton("Lock PC", Icons.Default.Lock, Color(0xFFEF4444), Modifier.weight(1f)) { onTriggerAction("LOCK") }
            DeckActionButton("Desktop", Icons.Default.DesktopWindows, Color(0xFF38BDF8), Modifier.weight(1f)) { onTriggerAction("DESKTOP") }
            DeckActionButton("Snip Tool", Icons.Default.Crop, Color(0xFFA78BFA), Modifier.weight(1f)) { onTriggerAction("SCREENSHOT") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckActionButton("Magnifier", Icons.Default.ScreenShare, Color(0xFF34D399), Modifier.weight(1f)) { onTriggerAction("MAGNIFIER") }
            DeckActionButton("Close App", Icons.Default.Lock, Color(0xFFF87171), Modifier.weight(1f)) { onTriggerAction("CLOSE_APP") }
            DeckActionButton("Format Code", Icons.Default.FormatPaint, Color(0xFF60A5FA), Modifier.weight(1f)) { onTriggerAction("FORMAT_CODE") }
        }

        // Category C: Media & Volume Controls
        Spacer(modifier = Modifier.height(4.dp))
        DeckSectionHeader("🎬 MEDIA & SOUND PLAYBACK")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckActionButton("Prev", Icons.Default.SkipPrevious, Color(0xFF94A3B8), Modifier.weight(1f)) { onTriggerAction("MEDIA_PREV") }
            DeckActionButton("Play/Pause", Icons.Default.PlayArrow, Color(0xFF38BDF8), Modifier.weight(1f)) { onTriggerAction("MEDIA_PLAY_PAUSE") }
            DeckActionButton("Next", Icons.Default.SkipNext, Color(0xFF94A3B8), Modifier.weight(1f)) { onTriggerAction("MEDIA_NEXT") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckActionButton("Mute", Icons.Default.VolumeMute, Color(0xFFF87171), Modifier.weight(1f)) { onTriggerAction("VOLUME_MUTE") }
            DeckActionButton("Vol -", Icons.Default.VolumeDown, Color(0xFF94A3B8), Modifier.weight(1f)) { onTriggerAction("VOLUME_DOWN") }
            DeckActionButton("Vol +", Icons.Default.VolumeUp, Color(0xFF34D399), Modifier.weight(1f)) { onTriggerAction("VOLUME_UP") }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DeckSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = GrayMuted,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DeckActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDark,
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WhitePrimary,
                maxLines = 1
            )
        }
    }
}
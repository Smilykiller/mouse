package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
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
import com.example.model.CustomMacroTile
import com.example.ui.dialogs.AddMacroDialog
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
    onSetSubTab: (String) -> Unit = {},
    onTriggerCustomMacro: (CustomMacroTile) -> Unit = {},
    onAddCustomMacro: (CustomMacroTile) -> Unit = {},
    onDeleteCustomMacro: (String) -> Unit = {},
    // Presenter & Gamepad callbacks
    onNextSlide: () -> Unit = {},
    onPrevSlide: () -> Unit = {},
    onStartSlideshow: () -> Unit = {},
    onEndSlideshow: () -> Unit = {},
    onBlankB: () -> Unit = {},
    onBlankW: () -> Unit = {},
    onToggleLaserPointer: (Boolean) -> Unit = {},
    onGamepadButton: (String) -> Unit = {},
    // Media callbacks
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrev: () -> Unit = {},
    onMediaMute: () -> Unit = {},
    onMediaVolumeUp: () -> Unit = {},
    onMediaVolumeDown: () -> Unit = {},
    onMediaRewind: () -> Unit = {},
    onMediaForward: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddMacroDialog by remember { mutableStateOf(false) }

    if (showAddMacroDialog) {
        AddMacroDialog(
            onDismiss = { showAddMacroDialog = false },
            onSaveTile = { tile -> onAddCustomMacro(tile) }
        )
    }

    // Switch between Sub-Tabs seamlessly
    when (state.deckActiveSubTab) {
        "PRESENT" -> {
            PresentationScreen(
                state = state,
                onBack = { onSetSubTab("DECK") },
                onNextSlide = onNextSlide,
                onPrevSlide = onPrevSlide,
                onStartSlideshow = onStartSlideshow,
                onEndSlideshow = onEndSlideshow,
                onBlankB = onBlankB,
                onBlankW = onBlankW,
                onToggleLaserPointer = onToggleLaserPointer
            )
            return
        }
        "GAMEPAD" -> {
            GamepadScreen(
                state = state,
                onBack = { onSetSubTab("DECK") },
                onButtonPress = onGamepadButton
            )
            return
        }
        "MEDIA" -> {
            MediaRemoteScreen(
                state = state,
                onBack = { onSetSubTab("DECK") },
                onPlayPause = onMediaPlayPause,
                onNext = onMediaNext,
                onPrev = onMediaPrev,
                onMute = onMediaMute,
                onVolumeUp = onMediaVolumeUp,
                onVolumeDown = onMediaVolumeDown,
                onRewind = onMediaRewind,
                onForward = onMediaForward,
                onLaunchSpotify = { onTriggerAction("SPOTIFY") }
            )
            return
        }
    }

    // Default: Stream Deck & Productivity Tools
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Top Header ---
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
                    text = "Stream deck, presentation, gamepad & media",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
            }
        }

        // --- Sub-Mode Selector Tabs ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeckSubModePill("🎛️ Deck", state.deckActiveSubTab == "DECK", Modifier.weight(1f)) { onSetSubTab("DECK") }
            DeckSubModePill("📽️ Presenter", state.deckActiveSubTab == "PRESENT", Modifier.weight(1.1f)) { onSetSubTab("PRESENT") }
            DeckSubModePill("🎮 Gamepad", state.deckActiveSubTab == "GAMEPAD", Modifier.weight(1f)) { onSetSubTab("GAMEPAD") }
            DeckSubModePill("🎵 Media", state.deckActiveSubTab == "MEDIA", Modifier.weight(1f)) { onSetSubTab("MEDIA") }
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Multi-Monitor Snap",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary
                        )
                        Text(
                            text = "Snap cursor instantly across displays",
                            fontSize = 11.5.sp,
                            color = GraySecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onJumpDisplay() }
                ) {
                    Text(
                        text = "Snap ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tab, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BROWSER TAB ROTARY WHEEL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayMuted,
                        letterSpacing = 1.sp
                    )
                }

                var dialOffset by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                dialOffset += dragAmount.x
                                if (dialOffset > 40f) {
                                    onSwitchTab(1)
                                    dialOffset = 0f
                                } else if (dialOffset < -40f) {
                                    onSwitchTab(-1)
                                    dialOffset = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("◀ Previous", fontSize = 12.sp, color = GraySecondary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF0284C7), Color(0xFF0F172A)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tab, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Next ▶", fontSize = 12.sp, color = GraySecondary)
                    }
                }
            }
        }

        // ==============================================================
        // 3. AUTO-SCROLLER / TELEPROMPTER
        // ==============================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TELEPROMPTER AUTO-SCROLLER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrayMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    if (state.isAutoScrolling) {
                        Text(
                            text = "● RUNNING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DotGreenMild
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSetAutoScrollSpeed(1.0f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.autoScrollSpeed == 1.0f) Color(0xFF0284C7) else DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Slow (1x)", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onSetAutoScrollSpeed(2.5f) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.autoScrollSpeed == 2.5f) Color(0xFF0284C7) else DarkSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Med (2x)", fontSize = 11.sp)
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
        // 4. USER CUSTOM MACRO TILES
        // ==============================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CUSTOM MACROS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayMuted, letterSpacing = 1.sp)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0284C7),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showAddMacroDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Tile", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Grid of Custom Macro Tiles
        val customTiles = state.customMacroTiles
        if (customTiles.isNotEmpty()) {
            for (chunk in customTiles.chunked(3)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunk.forEach { tile ->
                        CustomDeckButton(
                            tile = tile,
                            modifier = Modifier.weight(1f),
                            onClick = { onTriggerCustomMacro(tile) }
                        )
                    }
                    if (chunk.size < 3) {
                        for (i in 0 until (3 - chunk.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ==============================================================
        // 5. STANDARD STREAM DECK GRID
        // ==============================================================
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("STANDARD SHORTCUTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrayMuted, letterSpacing = 1.sp)
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DeckSubModePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF0284C7) else CardDark,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else BorderDark),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) Color.White else GraySecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CustomDeckButton(
    tile: CustomMacroTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = Color(tile.colorHex)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDark,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Bolt, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(tile.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePrimary, maxLines = 1)
            Text(tile.subtitle, fontSize = 9.sp, color = GraySecondary, maxLines = 1)
        }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
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

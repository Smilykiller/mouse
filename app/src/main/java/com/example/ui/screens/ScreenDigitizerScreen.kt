package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ScreenDigitizerScreen(
    state: AirGlideUiState,
    onBack: () -> Unit,
    onAbsoluteMove: (Float, Float) -> Unit,
    onAbsoluteClick: (Float, Float, String) -> Unit,
    onSafetyClear: () -> Unit,
    onToggleLiveMirror: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1. Force Landscape Orientation & Apply FLAG_SECURE Protection
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Enforce landscape
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Enforce FLAG_SECURE: blocks screenshots, screen recording, and app switcher preview
        try {
            activity?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (_: Exception) { }

        onDispose {
            try {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } catch (_: Exception) { }
            activity?.requestedOrientation = prevOrientation
            onSafetyClear()
        }
    }

    BackHandler {
        onSafetyClear()
        onBack()
    }

    val hostW = state.laptopTelemetry?.screenWidth?.takeIf { it > 0 } ?: 1920
    val hostH = state.laptopTelemetry?.screenHeight?.takeIf { it > 0 } ?: 1080
    val displayAspectRatio = (hostW.toFloat() / hostH.toFloat()).coerceIn(1.2f, 2.4f)

    var touchPosNorm by remember { mutableStateOf<Offset?>(null) }
    var mirroredBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isStreamStalled by remember { mutableStateOf(false) }

    // 2. Screen Mirror Frame Poller (Only active when isLiveMirrorActive == true)
    LaunchedEffect(state.isLiveMirrorActive, state.settings.targetHostIp) {
        if (!state.isLiveMirrorActive) {
            mirroredBitmap = null
            isStreamStalled = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val targetHost = state.settings.targetHostIp.ifBlank { "127.0.0.1" }
            val endpoint = "http://$targetHost:5056/screen"
            var failCount = 0

            while (isActive && state.isLiveMirrorActive) {
                try {
                    val url = URL(endpoint)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 800
                        readTimeout = 800
                        requestMethod = "GET"
                        useCaches = false
                    }

                    if (conn.responseCode == 200) {
                        conn.inputStream.use { stream: InputStream ->
                            val bytes = stream.readBytes()
                            if (bytes.isNotEmpty()) {
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bmp != null) {
                                    mirroredBitmap = bmp.asImageBitmap()
                                    failCount = 0
                                    isStreamStalled = false
                                }
                            }
                        }
                    } else {
                        failCount++
                    }
                } catch (e: Exception) {
                    failCount++
                }

                // If frames drop consecutively for > 1.5s, signal degraded/stalled stream
                if (failCount >= 3) {
                    isStreamStalled = true
                }

                kotlinx.coroutines.delay(65) // ~15 FPS lightweight screen feed
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- Top Bar (Compact Landscape Header) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button + Mode label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSafetyClear()
                            onBack()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WhitePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                    }
                }

                // Resolution Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "🖥️ ${hostW}×${hostH}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }

                // Live Touch Coordinate Readout
                touchPosNorm?.let { pos ->
                    val pixelX = (pos.x * hostW).toInt().coerceIn(0, hostW - 1)
                    val pixelY = (pos.y * hostH).toInt().coerceIn(0, hostH - 1)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F291E),
                        border = BorderStroke(1.dp, DotGreenMild),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "📍 $pixelX, $pixelY",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DotGreenMild,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Right: Dual Mode Toggle + FLAG_SECURE Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stream Status / Stalled Warning
                if (isStreamStalled && state.isLiveMirrorActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7F1D1D),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "⚠️ Network Slow",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                // Privacy Shield (FLAG_SECURE Active)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("SECURE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                }

                // Dual-Mode Toggle Button (Mirroring vs Blind Digitizer)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.isLiveMirrorActive) Color(0xFF0284C7) else ElevatedSurface,
                    border = BorderStroke(1.dp, if (state.isLiveMirrorActive) Color(0xFF38BDF8) else BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleLiveMirror() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isLiveMirrorActive) Icons.Default.Tv else Icons.Default.TvOff,
                            contentDescription = "Toggle Mirroring",
                            tint = if (state.isLiveMirrorActive) Color.White else GraySecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isLiveMirrorActive) "📺 Mirror ON" else "🎛️ Digitizer Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isLiveMirrorActive) Color.White else WhitePrimary
                        )
                    }
                }
            }
        }

        // --- Aspect-Ratio Constrained Display Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(displayAspectRatio)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF111827), Color(0xFF030712))
                        )
                    )
                    .border(BorderStroke(1.5.dp, Color(0xFF1E293B)), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val u = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val v = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                touchPosNorm = Offset(u, v)
                                onAbsoluteClick(u, v, "L")
                            },
                            onDoubleTap = { offset ->
                                val u = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val v = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                touchPosNorm = Offset(u, v)
                                onAbsoluteClick(u, v, "L")
                                onAbsoluteClick(u, v, "L")
                            },
                            onLongPress = { offset ->
                                val u = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val v = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                touchPosNorm = Offset(u, v)
                                onAbsoluteClick(u, v, "R")
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val u = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val v = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                touchPosNorm = Offset(u, v)
                                onAbsoluteMove(u, v)
                            },
                            onDragEnd = {
                                onSafetyClear()
                            },
                            onDragCancel = {
                                onSafetyClear()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val u = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val v = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                touchPosNorm = Offset(u, v)
                                onAbsoluteMove(u, v)
                            }
                        )
                    }
            ) {
                // Layer A: Live Mirroring Frame (If active and available)
                if (state.isLiveMirrorActive && mirroredBitmap != null) {
                    Image(
                        bitmap = mirroredBitmap!!,
                        contentDescription = "Desktop Mirror",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Layer B: Minimalist Coordinate Grid & Touch Feedback Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // If in Blind Digitizer mode: draw subtle quadrant divisions
                    if (!state.isLiveMirrorActive || mirroredBitmap == null) {
                        val gridColor = Color(0xFF38BDF8).copy(alpha = 0.12f)
                        drawLine(gridColor, Offset(w / 2f, 0f), Offset(w / 2f, h), 1.2f)
                        drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), 1.2f)
                    }

                    // Tactile Touch Indicator Bloom
                    touchPosNorm?.let { posNorm ->
                        val px = posNorm.x * w
                        val py = posNorm.y * h
                        val touchCenter = Offset(px, py)

                        // Ambient glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color.Transparent),
                                center = touchCenter,
                                radius = 28.dp.toPx()
                            ),
                            radius = 28.dp.toPx(),
                            center = touchCenter
                        )
                        // Precision pip
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 4.dp.toPx(),
                            center = touchCenter
                        )
                    }
                }

                // Layer C: Fallback / Guidance Text if no mirror frame is active
                if (!state.isLiveMirrorActive || mirroredBitmap == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "👆 Tap to Click • Slide for Absolute Precision Cursor • Hold for Right-Click",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = GraySecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

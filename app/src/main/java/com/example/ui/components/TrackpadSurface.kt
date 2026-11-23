package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MousePadTheme
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.WhitePrimary
import java.io.File

@Composable
fun TrackpadSurface(
    onPointerMove: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    enableTapToClick: Boolean = true,
    label: String = "TRACKPAD SURFACE",
    onScroll: (Float) -> Unit = {},
    onLeftDown: () -> Unit = {},
    onLeftUp: () -> Unit = {},
    onZoom: (Boolean) -> Unit = {},
    onWinTab: () -> Unit = {},
    onWinD: () -> Unit = {},
    onAltTab: () -> Unit = {},
    onWinA: () -> Unit = {},
    theme: MousePadTheme = MousePadTheme.STEALTH,
    customImagePath: String? = null,
    dragLockActive: Boolean = false,
    onToggleDragLock: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = remember(context) { com.example.util.HapticFeedbackHelper(context) }

    val currentOnPointerMove by rememberUpdatedState(onPointerMove)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnScroll by rememberUpdatedState(onScroll)
    val currentOnLeftDown by rememberUpdatedState(onLeftDown)
    val currentOnLeftUp by rememberUpdatedState(onLeftUp)
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnWinTab by rememberUpdatedState(onWinTab)
    val currentOnWinD by rememberUpdatedState(onWinD)
    val currentOnAltTab by rememberUpdatedState(onAltTab)
    val currentOnWinA by rememberUpdatedState(onWinA)

    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }
    var isDragActive by remember { mutableStateOf(false) }

    var lastTapUpTime by remember { mutableStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Load custom gallery image if selected
    val customBitmap = remember(customImagePath, theme) {
        if (theme == MousePadTheme.CUSTOM_PHOTO && !customImagePath.isNullOrBlank()) {
            val file = File(customImagePath)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    // Determine theme base colors
    val palette = remember(theme) {
        when (theme) {
            MousePadTheme.STEALTH -> PadPalette(
                bgTop = Color(0xFF131D2E),
                bgBottom = Color(0xFF090D15),
                accent = Color(0xFF38BDF8),
                border = Color(0xFF1E293B),
                ripple = Color(0xFF38BDF8)
            )
            MousePadTheme.CYBER_NEON -> PadPalette(
                bgTop = Color(0xFF0D172E),
                bgBottom = Color(0xFF040711),
                accent = Color(0xFF06B6D4),
                border = Color(0xFF0891B2),
                ripple = Color(0xFF22D3EE)
            )
            MousePadTheme.FROSTED_GLASS -> PadPalette(
                bgTop = Color(0xFF243044),
                bgBottom = Color(0xFF0E1622),
                accent = Color(0xFF94A3B8),
                border = Color(0xFF475569),
                ripple = Color(0xFFF1F5F9)
            )
            MousePadTheme.CARBON_FIBER -> PadPalette(
                bgTop = Color(0xFF1E2026),
                bgBottom = Color(0xFF0D0E11),
                accent = Color(0xFFF59E0B),
                border = Color(0xFF3F3F46),
                ripple = Color(0xFFF59E0B)
            )
            MousePadTheme.SUNSET_AURA -> PadPalette(
                bgTop = Color(0xFF35114E),
                bgBottom = Color(0xFF12031F),
                accent = Color(0xFFF43F5E),
                border = Color(0xFFBE185D),
                ripple = Color(0xFFFB7185)
            )
            MousePadTheme.EMERALD_MATRIX -> PadPalette(
                bgTop = Color(0xFF04261B),
                bgBottom = Color(0xFF010E09),
                accent = Color(0xFF10B981),
                border = Color(0xFF059669),
                ripple = Color(0xFF34D399)
            )
            MousePadTheme.RETRO_SYNTHWAVE -> PadPalette(
                bgTop = Color(0xFF280B45),
                bgBottom = Color(0xFF0B0216),
                accent = Color(0xFFE879F9),
                border = Color(0xFFC026D3),
                ripple = Color(0xFF38BDF8)
            )
            MousePadTheme.NORDIC_MINIMALIST -> PadPalette(
                bgTop = Color(0xFF252C37),
                bgBottom = Color(0xFF12161D),
                accent = Color(0xFFCBD5E1),
                border = Color(0xFF475569),
                ripple = Color(0xFFF8FAFC)
            )
            MousePadTheme.TITANIUM_METAL -> PadPalette(
                bgTop = Color(0xFF262E3B),
                bgBottom = Color(0xFF13171F),
                accent = Color(0xFF38BDF8),
                border = Color(0xFF475569),
                ripple = Color(0xFF38BDF8)
            )
            MousePadTheme.DEEP_SPACE -> PadPalette(
                bgTop = Color(0xFF181B42),
                bgBottom = Color(0xFF060818),
                accent = Color(0xFF818CF8),
                border = Color(0xFF4F46E5),
                ripple = Color(0xFFA5B4FC)
            )
            MousePadTheme.TOKYO_MIDNIGHT -> PadPalette(
                bgTop = Color(0xFF260D38),
                bgBottom = Color(0xFF070412),
                accent = Color(0xFFEC4899),
                border = Color(0xFFBE185D),
                ripple = Color(0xFF38BDF8)
            )
            MousePadTheme.AURORA_BOREALIS -> PadPalette(
                bgTop = Color(0xFF06332E),
                bgBottom = Color(0xFF021312),
                accent = Color(0xFF14B8A6),
                border = Color(0xFF0D9488),
                ripple = Color(0xFFA855F7)
            )
            MousePadTheme.AMOLED_PITCH -> PadPalette(
                bgTop = Color(0xFF080808),
                bgBottom = Color(0xFF000000),
                accent = Color(0xFFE4E4E7),
                border = Color(0xFF27272A),
                ripple = Color(0xFFFFFFFF)
            )
            MousePadTheme.CUSTOM_PHOTO -> PadPalette(
                bgTop = Color(0xFF12131A),
                bgBottom = Color(0xFF08090C),
                accent = Color(0xFF38BDF8),
                border = Color(0xFF38BDF8),
                ripple = Color(0xFF38BDF8)
            )
        }
    }

    val activeBorderColor = if (dragLockActive) Color(0xFFF59E0B) else palette.border
    val activeBorderWidth = if (dragLockActive) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(palette.bgTop, palette.bgBottom)))
            .border(activeBorderWidth, activeBorderColor, RoundedCornerShape(20.dp))
            .testTag("trackpad_surface")
            .pointerInput(enableTapToClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPosition = down.position
                    isTouching = true
                    lastInteractionTime = System.currentTimeMillis()
                    val downTime = System.currentTimeMillis()
                    var hasMoved = false
                    var usedTwoFingers = false
                    var twoFingersMoved = false
                    var isPinchZooming = false
                    var isTwoFingerScrolling = false
                    var usedThreeFingers = false
                    var threeFingersTriggered = false
                    var usedFourFingers = false
                    var lastPosition = down.position
                    var lastCenterY = 0f
                    var initialSpan = 0f
                    var lastSpan = 0f
                    var threeFingerStartY = 0f
                    var threeFingerStartX = 0f

                    // 1. Double-Tap and Drag Recognition
                    val isDoubleTapDrag = (downTime - lastTapUpTime < 320L) &&
                            (down.position - lastTapPosition).getDistance() < 140f

                    var draggingStarted = false
                    if (isDoubleTapDrag) {
                        draggingStarted = true
                        isDragActive = true
                        haptic.heavyClick()
                        currentOnLeftDown()
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            val duration = System.currentTimeMillis() - downTime
                            if (draggingStarted || isDragActive) {
                                currentOnLeftUp()
                                isDragActive = false
                                draggingStarted = false
                                lastTapUpTime = 0L
                            } else if (usedFourFingers && duration < 350L) {
                                // 4-Finger Tap: Action Center
                                haptic.heavyClick()
                                currentOnWinA()
                            } else if (usedTwoFingers && !twoFingersMoved && duration < 380L) {
                                // Two-finger tap: Right Click
                                haptic.heavyClick()
                                currentOnLongPress()
                            } else if (!hasMoved && !usedTwoFingers && !usedThreeFingers && !usedFourFingers && enableTapToClick) {
                                if (duration < 280L) {
                                    lastTapUpTime = System.currentTimeMillis()
                                    lastTapPosition = down.position
                                    haptic.click()
                                    currentOnTap()
                                } else if (duration >= 500L) {
                                    haptic.heavyClick()
                                    currentOnLongPress()
                                }
                            }
                            break
                        }

                        // --- 4-Finger Gesture ---
                        if (pressed.size >= 4) {
                            usedFourFingers = true
                            pressed.forEach { it.consume() }
                            continue
                        }

                        // --- 3-Finger Windows Gestures ---
                        if (pressed.size == 3) {
                            val avgX = pressed.map { it.position.x }.average().toFloat()
                            val avgY = pressed.map { it.position.y }.average().toFloat()
                            if (!usedThreeFingers) {
                                usedThreeFingers = true
                                threeFingerStartX = avgX
                                threeFingerStartY = avgY
                            } else if (!threeFingersTriggered) {
                                val dx = avgX - threeFingerStartX
                                val dy = avgY - threeFingerStartY
                                if (dy < -65f) {
                                    // Swipe UP -> Task View
                                    haptic.click()
                                    currentOnWinTab()
                                    threeFingersTriggered = true
                                } else if (dy > 65f) {
                                    // Swipe DOWN -> Show Desktop
                                    haptic.click()
                                    currentOnWinD()
                                    threeFingersTriggered = true
                                } else if (Math.abs(dx) > 65f) {
                                    // Swipe LEFT/RIGHT -> Switch App
                                    haptic.click()
                                    currentOnAltTab()
                                    threeFingersTriggered = true
                                }
                            }
                            pressed.forEach { it.consume() }
                            continue
                        }

                        // --- 2-Finger Gestures (Scroll, Pinch Zoom & Right Click) ---
                        if (pressed.size == 2) {
                            val span = (pressed[0].position - pressed[1].position).getDistance()
                            val centerY = pressed.map { it.position.y }.average().toFloat()

                            if (!usedTwoFingers) {
                                usedTwoFingers = true
                                lastCenterY = centerY
                                initialSpan = span
                                lastSpan = span
                            } else {
                                val spanDiff = span - lastSpan
                                val totalSpanDelta = Math.abs(span - initialSpan)

                                if (!isTwoFingerScrolling && (isPinchZooming || totalSpanDelta > 32f)) {
                                    // Pinch to Zoom mode locked: strictly zooms, never triggers accidental scroll
                                    isPinchZooming = true
                                    twoFingersMoved = true
                                    if (Math.abs(spanDiff) > 8f) {
                                        currentOnZoom(spanDiff > 0)
                                        lastSpan = span
                                    }
                                    lastCenterY = centerY
                                } else if (!isPinchZooming) {
                                    val dy = centerY - lastCenterY
                                    lastCenterY = centerY
                                    if (Math.abs(dy) > 1.2f) {
                                        isTwoFingerScrolling = true
                                        twoFingersMoved = true
                                        if (Math.abs(dy) > 6f) haptic.scrollTick()
                                        currentOnScroll(dy)
                                    }
                                }
                            }
                            pressed.forEach { it.consume() }
                            continue
                        }

                        if (usedTwoFingers || usedThreeFingers || usedFourFingers) {
                            lastPosition = pressed[0].position
                            pressed[0].consume()
                            continue
                        }

                        // --- 1-Finger Cursor Movement & Hold-to-Drag ---
                        val change = pressed[0]
                        val currentPos = change.position
                        val dx = currentPos.x - lastPosition.x
                        val dy = currentPos.y - lastPosition.y
                        val elapsed = System.currentTimeMillis() - downTime

                        // Press and Hold in place for > 380ms initiates drag mode
                        if (!hasMoved && !draggingStarted && elapsed > 380L) {
                            draggingStarted = true
                            isDragActive = true
                            haptic.heavyClick()
                            currentOnLeftDown()
                        }

                        if (Math.abs(dx) > 0.01f || Math.abs(dy) > 0.01f) {
                            hasMoved = true
                            lastPosition = currentPos
                            touchPosition = currentPos
                            change.consume()
                            currentOnPointerMove(dx, dy)
                        }
                    }

                    isTouching = false
                    touchPosition = null
                    if (isDragActive) {
                        currentOnLeftUp()
                        isDragActive = false
                    }
                }
            }
    ) {
        // 1. Custom Image Background (if chosen by user)
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Trackpad Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.65f)
            )
            // Soft overlay to maintain UI readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // 2. Clean, elegant tactile touch indicator canvas (No AI graph/brackets/grids)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Gentle ambient light bloom following touch (soft, natural physical feel)
            touchPosition?.let { pos ->
                val activeGlow = if (dragLockActive) Color(0xFFF59E0B) else palette.ripple

                // Soft outer ambient bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeGlow.copy(alpha = 0.28f),
                            activeGlow.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = pos,
                        radius = 52.dp.toPx()
                    ),
                    radius = 52.dp.toPx(),
                    center = pos
                )
                // Small subtle tactile contact pip
                drawCircle(
                    color = activeGlow.copy(alpha = 0.75f),
                    radius = 4.dp.toPx(),
                    center = pos
                )
            }
        }

        // 3. User-Friendly Floating Gesture Guidance Chip at Bottom (Minimalist & unobtrusive)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (dragLockActive) "🔒 Dragging • Tap to Drop"
                           else "👆 1-Finger Tap to Click • ✌️ 2-Finger Scroll & Zoom • 2-Tap Drag",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dragLockActive) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                    letterSpacing = 0.3.sp
                )
            }
        }

        // 4. Drag Lock Floating Banner (When Active in ANY Layout)
        if (dragLockActive) {
            Surface(
                shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                color = Color(0xFFB45309),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                    .clickable { onToggleDragLock() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Dragging Active",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DRAGGING ACTIVE • TAP TO DROP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private data class PadPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
    val border: Color,
    val ripple: Color
)

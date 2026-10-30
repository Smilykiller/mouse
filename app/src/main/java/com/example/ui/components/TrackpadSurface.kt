package com.example.ui.components

import android.graphics.BitmapFactory
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
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    enableTapToClick: Boolean = true,
    label: String = "TRACKPAD SURFACE",
    onScroll: (Float) -> Unit = {},
    theme: MousePadTheme = MousePadTheme.STEALTH,
    customImagePath: String? = null,
    dragLockActive: Boolean = false,
    onToggleDragLock: () -> Unit = {}
) {
    val currentOnPointerMove by rememberUpdatedState(onPointerMove)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnScroll by rememberUpdatedState(onScroll)

    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }

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
    val (bgColor, gridColor, baseBorderColor, rippleColor) = remember(theme) {
        when (theme) {
            MousePadTheme.CYBER_NEON -> QuadColors(
                Color(0xFF070B14),
                Color(0xFF06B6D4).copy(alpha = 0.35f),
                Color(0xFF06B6D4),
                Color(0xFF22D3EE)
            )
            MousePadTheme.FROSTED_GLASS -> QuadColors(
                Color(0xFF1E293B),
                Color(0xFF94A3B8).copy(alpha = 0.25f),
                Color(0xFF94A3B8).copy(alpha = 0.5f),
                Color(0xFFE2E8F0)
            )
            MousePadTheme.CARBON_FIBER -> QuadColors(
                Color(0xFF141417),
                Color(0xFF3F3F46).copy(alpha = 0.35f),
                Color(0xFF52525B),
                Color(0xFFA1A1AA)
            )
            MousePadTheme.SUNSET_AURA -> QuadColors(
                Color(0xFF2E1065),
                Color(0xFFA855F7).copy(alpha = 0.30f),
                Color(0xFFA855F7),
                Color(0xFFF472B6)
            )
            MousePadTheme.EMERALD_MATRIX -> QuadColors(
                Color(0xFF021B10),
                Color(0xFF10B981).copy(alpha = 0.35f),
                Color(0xFF10B981),
                Color(0xFF34D399)
            )
            MousePadTheme.CUSTOM_PHOTO -> QuadColors(
                Color(0xFF12131A),
                Color.White.copy(alpha = 0.25f),
                Color(0xFF38BDF8),
                Color(0xFF38BDF8)
            )
            MousePadTheme.STEALTH -> QuadColors(
                CardDark,
                BorderDark.copy(alpha = 0.4f),
                BorderDark,
                WhitePrimary
            )
        }
    }

    val activeBorderColor = if (dragLockActive) Color(0xFFF59E0B) else baseBorderColor
    val activeBorderWidth = if (dragLockActive) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(activeBorderWidth, activeBorderColor, RoundedCornerShape(16.dp))
            .testTag("trackpad_surface")
            .pointerInput(enableTapToClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPosition = down.position
                    isTouching = true
                    val downTime = System.currentTimeMillis()
                    var hasMoved = false
                    var usedTwoFingers = false
                    var lastPosition = down.position
                    var lastCenterY = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            val duration = System.currentTimeMillis() - downTime
                            if (!hasMoved && !usedTwoFingers && enableTapToClick) {
                                if (duration < 380L) {
                                    currentOnTap()
                                } else if (duration >= 550L) {
                                    currentOnLongPress()
                                }
                            }
                            break
                        }

                        if (pressed.size >= 2) {
                            val centerY = pressed.map { it.position.y }.average().toFloat()
                            if (!usedTwoFingers) {
                                usedTwoFingers = true
                                lastCenterY = centerY
                            } else {
                                val dy = centerY - lastCenterY
                                lastCenterY = centerY
                                if (Math.abs(dy) > 0.01f) {
                                    currentOnScroll(dy)
                                }
                            }
                            pressed.forEach { it.consume() }
                            continue
                        }

                        if (usedTwoFingers) {
                            lastPosition = pressed[0].position
                            pressed[0].consume()
                            continue
                        }

                        val change = pressed[0]
                        val currentPos = change.position
                        val dx = currentPos.x - lastPosition.x
                        val dy = currentPos.y - lastPosition.y

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
        } else if (theme == MousePadTheme.SUNSET_AURA) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2E1065), Color(0xFF4C0519), Color(0xFF0F172A))
                        )
                    )
            )
        } else if (theme == MousePadTheme.FROSTED_GLASS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
            )
        }

        // 2. High-precision minimalist grid & glowing touch indicator canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Tech grid lines
            val gridSpacing = 44.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }

            // Crosshair markers for Cyber Neon and Emerald Matrix themes
            if (theme == MousePadTheme.CYBER_NEON || theme == MousePadTheme.EMERALD_MATRIX) {
                val cx = width / 2f
                val cy = height / 2f
                val arm = 14.dp.toPx()
                drawLine(rippleColor.copy(alpha = 0.5f), Offset(cx - arm, cy), Offset(cx + arm, cy), 1.5f)
                drawLine(rippleColor.copy(alpha = 0.5f), Offset(cx, cy - arm), Offset(cx, cy + arm), 1.5f)
                drawCircle(rippleColor.copy(alpha = 0.3f), radius = 24.dp.toPx(), center = Offset(cx, cy), style = Stroke(1f))
            }

            // Touch wave indicator
            touchPosition?.let { pos ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(rippleColor.copy(alpha = 0.45f), rippleColor.copy(alpha = 0.1f), Color.Transparent),
                        center = pos,
                        radius = 70.dp.toPx()
                    ),
                    radius = 70.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = rippleColor,
                    radius = 5.dp.toPx(),
                    center = pos
                )
            }
        }

        // 3. Center Hint Label
        Text(
            text = if (dragLockActive) "DRAGGING ACTIVE • SWIPE TO DRAG" else label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (dragLockActive) Color(0xFFFBBF24) else GrayMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )

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

private data class QuadColors(
    val bg: Color,
    val grid: Color,
    val border: Color,
    val ripple: Color
)

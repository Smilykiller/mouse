package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.WhitePrimary

@Composable
fun TrackpadSurface(
    onPointerMove: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    enableTapToClick: Boolean = true,
    label: String = "TRACKPAD SURFACE"
) {
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .testTag("trackpad_surface")
            .pointerInput(enableTapToClick) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPosition = down.position
                    isTouching = true
                    val downTime = System.currentTimeMillis()
                    var hasMoved = false
                    var lastPosition = down.position
                    var tapCandidate = true

                    while (true) {
                        val event = awaitPointerEvent()
                        val currentChange = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!currentChange.pressed) {
                            // Finger lifted cleanly
                            val duration = System.currentTimeMillis() - downTime
                            if (!hasMoved && tapCandidate && duration < 380L) {
                                currentChange.consume()
                                if (enableTapToClick) {
                                    onTap()
                                }
                            }
                            break
                        }

                        val currentPos = currentChange.position
                        val dx = currentPos.x - lastPosition.x
                        val dy = currentPos.y - lastPosition.y

                        if (Math.abs(dx) > 0.01f || Math.abs(dy) > 0.01f) {
                            hasMoved = true
                            tapCandidate = false
                            lastPosition = currentPos
                            touchPosition = currentPos
                            currentChange.consume()
                            onPointerMove(dx, dy)
                        }
                    }

                    isTouching = false
                    touchPosition = null
                }
            }
    ) {
        // High-precision minimalist grid & glowing touch indicator canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Subtle graphite grid
            val gridSpacing = 44.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = BorderDark.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = BorderDark.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }

            // Outer border
            drawRoundRect(
                color = BorderDark,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Touch wave indicator in monochromatic white/silver
            touchPosition?.let { pos ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(WhitePrimary.copy(alpha = 0.4f), WhitePrimary.copy(alpha = 0.08f), Color.Transparent),
                        center = pos,
                        radius = 70.dp.toPx()
                    ),
                    radius = 70.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = WhitePrimary,
                    radius = 5.dp.toPx(),
                    center = pos
                )
            }
        }

        // Center Hint Label
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = GrayMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        )
    }
}

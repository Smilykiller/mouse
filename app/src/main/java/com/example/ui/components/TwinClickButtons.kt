package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary

@Composable
fun TwinClickButtons(
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMiddleClick: () -> Unit,
    dragLockActive: Boolean,
    onToggleDragLock: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 120
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Drag Lock Pill Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (dragLockActive) ElevatedSurface else CardDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dragLockActive) WhitePrimary else BorderDark
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleDragLock() }
                    .testTag("drag_lock_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (dragLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Drag Lock",
                        tint = if (dragLockActive) WhitePrimary else GrayMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (dragLockActive) "DRAG LOCK ACTIVE (HELD)" else "DRAG LOCK (TAP TO HOLD)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dragLockActive) WhitePrimary else GraySecondary
                    )
                }
            }

            // Middle Click Quick Action
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMiddleClick() }
                    .testTag("middle_click_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mouse,
                        contentDescription = "Middle Click",
                        tint = GraySecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MID CLICK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GraySecondary
                    )
                }
            }
        }

        // Two Large Ergonomic Boxes (Monochrome high contrast)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Click (Primary)
            MonochromeClickBox(
                label = "LEFT CLICK",
                sublabel = if (dragLockActive) "LOCKED" else "PRIMARY",
                isLocked = dragLockActive,
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxSize()
                    .testTag("left_click_box"),
                onClick = onLeftClick
            )

            // Right Click
            MonochromeClickBox(
                label = "RIGHT CLICK",
                sublabel = "SECONDARY",
                isLocked = false,
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxSize()
                    .testTag("right_click_box"),
                onClick = onRightClick
            )
        }
    }
}

@Composable
private fun MonochromeClickBox(
    label: String,
    sublabel: String,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor = when {
        isPressed -> ElevatedSurface
        isLocked -> ElevatedSurface
        else -> CardDark
    }
    val borderColor = when {
        isPressed -> WhitePrimary
        isLocked -> WhitePrimary
        else -> BorderDark
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WhitePrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sublabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPressed || isLocked) WhitePrimary else GrayMuted
            )
        }
    }
}

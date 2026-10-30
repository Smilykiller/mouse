package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CustomMacroTile
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.WhitePrimary
import java.util.UUID

@Composable
fun AddMacroDialog(
    onDismiss: () -> Unit,
    onSaveTile: (CustomMacroTile) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hotkey by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF0284C7) }
    var selectedType by remember { mutableStateOf("HOTKEY") }

    val presetColors = listOf(
        0xFF0284C7, // Cyan
        0xFF8B5CF6, // Purple
        0xFF10B981, // Emerald
        0xFFF59E0B, // Amber
        0xFFEF4444, // Red
        0xFFEC4899  // Pink
    )

    val commonHotkeys = listOf(
        "CTRL+C", "CTRL+V", "CTRL+Z", "WIN+SHIFT+S", "CTRL+SHIFT+ESC", "ALT+F4", "WIN+D", "WIN+L"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardDark,
            border = BorderStroke(1.5.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "✨ CREATE CUSTOM MACRO TILE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF38BDF8)
                )

                // 1. Button Title
                Column {
                    Text("TILE LABEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GraySecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Snipping Tool, Git Push", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = WhitePrimary,
                            unfocusedTextColor = WhitePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Hotkey or Shortcut Combo
                Column {
                    Text("SHORTCUT / HOTKEY COMBO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GraySecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = hotkey,
                        onValueChange = { hotkey = it },
                        placeholder = { Text("e.g. WIN+SHIFT+S or CTRL+C", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = WhitePrimary,
                            unfocusedTextColor = WhitePrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick chip recommendations
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonHotkeys.take(4).forEach { hk ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElevatedSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { hotkey = hk }
                            ) {
                                Text(
                                    text = hk,
                                    fontSize = 9.5.sp,
                                    color = GraySecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Tile Color Accent
                Column {
                    Text("COLOR ACCENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GraySecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        presetColors.forEach { col ->
                            val isSelected = selectedColor == col
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(
                                        BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.Transparent),
                                        CircleShape
                                    )
                                    .clickable { selectedColor = col }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Cancel / Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = GraySecondary)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && hotkey.isNotBlank()) {
                                onSaveTile(
                                    CustomMacroTile(
                                        id = UUID.randomUUID().toString(),
                                        title = title.trim(),
                                        subtitle = hotkey.trim().uppercase(),
                                        iconName = "bolt",
                                        colorHex = selectedColor,
                                        actionType = selectedType,
                                        actionPayload = hotkey.trim().uppercase()
                                    )
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Tile", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

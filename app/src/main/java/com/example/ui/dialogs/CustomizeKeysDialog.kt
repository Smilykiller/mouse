package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.KeyItem
import com.example.model.KeyboardCatalog
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizeKeysDialog(
    currentKeys: List<String>,
    onSaveKeys: (List<String>) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedKeys = remember {
        mutableStateListOf<String>().apply {
            addAll(currentKeys.take(8))
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Controls", "Navigation", "Editing", "Function")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ElevatedSurface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Customize Keys",
                                    tint = DotGreenMild,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Customize Quick Keys",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Choose up to 8 buttons (Top 4 & Bottom 4)",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GrayMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable content body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Preview of the 2 Layers
                    Surface(
                        color = CardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTIVE BUTTONS LAYOUT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrayMuted,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${selectedKeys.size}/8 selected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedKeys.size == 8) DotGreenMild else GraySecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Top 4 row
                            Text(
                                text = "Top Layer (Buttons 1–4):",
                                fontSize = 10.sp,
                                color = GraySecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 0 until 4) {
                                    val code = selectedKeys.getOrNull(i)
                                    ActiveSlotChip(
                                        code = code,
                                        onRemove = { if (code != null) selectedKeys.remove(code) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom 4 row
                            Text(
                                text = "Bottom Layer (Buttons 5–8):",
                                fontSize = 10.sp,
                                color = GraySecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 4 until 8) {
                                    val code = selectedKeys.getOrNull(i)
                                    ActiveSlotChip(
                                        code = code,
                                        onRemove = { if (code != null) selectedKeys.remove(code) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) ElevatedSurface else CardDark,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) WhitePrimary else BorderDark
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) WhitePrimary else GraySecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Available Keys Grid
                    val filteredKeys = if (selectedCategory == "All") {
                        KeyboardCatalog.ALL_KEYS
                    } else {
                        KeyboardCatalog.ALL_KEYS.filter { it.category == selectedCategory }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredKeys.forEach { keyItem ->
                            val isAdded = selectedKeys.contains(keyItem.code)
                            KeyPickerChip(
                                item = keyItem,
                                isSelected = isAdded,
                                onClick = {
                                    if (isAdded) {
                                        selectedKeys.remove(keyItem.code)
                                    } else {
                                        if (selectedKeys.size < 8) {
                                            selectedKeys.add(keyItem.code)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedKeys.clear()
                            selectedKeys.addAll(KeyboardCatalog.DEFAULT_KEY_CODES)
                            onResetDefault()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardDark,
                            contentColor = WhitePrimary
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = GraySecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default", fontSize = 12.sp, color = WhitePrimary)
                    }

                    Button(
                        onClick = {
                            onSaveKeys(selectedKeys.toList())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhitePrimary,
                            contentColor = TrueBlack
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("save_custom_keys_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = TrueBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Keys (${selectedKeys.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrueBlack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSlotChip(
    code: String?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (code != null) {
        val item = KeyboardCatalog.find(code)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = ElevatedSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = modifier
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onRemove() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePrimary,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .height(34.dp)
                .background(Color(0xFF141416), RoundedCornerShape(6.dp))
                .border(
                    1.dp,
                    BorderDark,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Empty",
                fontSize = 10.sp,
                color = GrayMuted
            )
        }
    }
}

@Composable
private fun KeyPickerChip(
    item: KeyItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF122818) else CardDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) DotGreenMild else BorderDark
        ),
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = DotGreenMild,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = item.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) DotGreenMild else WhitePrimary
            )
        }
    }
}

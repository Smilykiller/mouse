package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyboardCatalog
import com.example.ui.dialogs.CustomizeKeysDialog
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

@Composable
fun RemoteKeyboardBar(
    isOpen: Boolean,
    customKeyCodes: List<String>,
    focusRequester: FocusRequester,
    onSendText: (String) -> Unit,
    onKeyPress: (String) -> Unit,
    onUpdateQuickKeys: (List<String>) -> Unit,
    onResetQuickKeys: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showCustomizeDialog by remember { mutableStateOf(false) }

    // Interactive Typing State
    var liveFieldValue by remember { mutableStateOf(TextFieldValue(" ", TextRange(1))) }
    var isDraftMode by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Section: 2-Layer Grid of Custom Quick Keys (Top 4 + Bottom 4) + End Plus (+) Button
                val activeItems = customKeyCodes.take(8).map { KeyboardCatalog.find(it) }
                val topRowKeys = activeItems.take(4)
                val bottomRowKeys = activeItems.drop(4).take(4)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 2-Layer Layout: Top 4 buttons and Bottom 4 buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Top Layer (up to 4)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            topRowKeys.forEach { keyItem ->
                                SpecialKeyButton(
                                    label = keyItem.label,
                                    onClick = { onKeyPress(keyItem.code) }
                                )
                            }
                        }

                        // Bottom Layer (up to 4)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomRowKeys.forEach { keyItem ->
                                SpecialKeyButton(
                                    label = keyItem.label,
                                    onClick = { onKeyPress(keyItem.code) }
                                )
                            }
                        }
                    }

                    // Plus (+) Button: appears at the end when scrolled to customize all buttons
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardDark,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, BorderHighlight),
                        modifier = Modifier
                            .height(74.dp)
                            .width(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCustomizeDialog = true }
                            .testTag("open_key_customizer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Customize Additional Keys (+)",
                                tint = DotGreenMild,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector Bar: [⚡ Live Keys] / [📝 Text & Voice]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (!isDraftMode) DotGreenMild.copy(alpha = 0.2f) else CardDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (!isDraftMode) DotGreenMild else BorderDark),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isDraftMode = false }
                        ) {
                            Text(
                                text = "⚡ Live Typing",
                                fontSize = 11.sp,
                                fontWeight = if (!isDraftMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isDraftMode) DotGreenMild else GraySecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDraftMode) DotGreenMild.copy(alpha = 0.2f) else CardDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDraftMode) DotGreenMild else BorderDark),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isDraftMode = true }
                        ) {
                            Text(
                                text = "📝 Draft & Voice",
                                fontSize = 11.sp,
                                fontWeight = if (isDraftMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDraftMode) DotGreenMild else GraySecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Hide Keyboard Button
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onClose()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Hide Keyboard",
                            tint = DotGreenMild,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Section: Input Field based on selected mode
                if (!isDraftMode) {
                    // --- LIVE TYPING MODE (Direct keystrokes to PC, 100% IME safe) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = liveFieldValue,
                            onValueChange = { incoming ->
                                val oldText = liveFieldValue.text
                                val newText = incoming.text

                                if (newText.length > oldText.length) {
                                    val added = if (newText.startsWith(oldText)) {
                                        newText.substring(oldText.length)
                                    } else {
                                        newText.replace(" ", "")
                                    }
                                    if (added.isNotEmpty()) onSendText(added)
                                    liveFieldValue = TextFieldValue(" ", TextRange(1))
                                } else if (newText.length < oldText.length) {
                                    val deletedCount = (oldText.length - newText.length).coerceAtLeast(1)
                                    repeat(deletedCount) {
                                        onKeyPress("BACKSPACE")
                                    }
                                    liveFieldValue = TextFieldValue(" ", TextRange(1))
                                } else {
                                    if (newText != " ") {
                                        val typed = newText.replace(" ", "")
                                        if (typed.isNotEmpty()) onSendText(typed)
                                        liveFieldValue = TextFieldValue(" ", TextRange(1))
                                    } else {
                                        liveFieldValue = incoming
                                    }
                                }
                            },
                            placeholder = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(DotGreenMild)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Live typing • ⌫ backspace • ↵ enter",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GrayMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DotGreenMild,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = WhitePrimary,
                                unfocusedTextColor = WhitePrimary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onKeyPress("ENTER")
                                    liveFieldValue = TextFieldValue(" ", TextRange(1))
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Backspace || event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL)) {
                                        onKeyPress("BACKSPACE")
                                        liveFieldValue = TextFieldValue(" ", TextRange(1))
                                        true
                                    } else false
                                }
                                .testTag("remote_text_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        SpecialKeyButton(
                            label = "↵ Enter",
                            onClick = { onKeyPress("ENTER") }
                        )
                    }
                } else {
                    // --- DRAFT & VOICE MODE (Full sentence composition, copy-paste, voice dictation) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            placeholder = {
                                Text(
                                    text = "Type sentence or use voice input...",
                                    fontSize = 12.sp,
                                    color = GrayMuted
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DotGreenMild,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = WhitePrimary,
                                unfocusedTextColor = WhitePrimary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (draftText.isNotEmpty()) {
                                        onSendText(draftText)
                                        draftText = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("remote_draft_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (draftText.isNotEmpty()) {
                            IconButton(
                                onClick = { draftText = "" },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Draft",
                                    tint = GraySecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                if (draftText.isNotEmpty()) {
                                    onSendText(draftText)
                                    draftText = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DotGreenMild),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DotGreenMild.copy(alpha = 0.15f),
                                contentColor = DotGreenMild
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(text = "Send ↵", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizeKeysDialog(
            currentKeys = customKeyCodes,
            onSaveKeys = { newKeys -> onUpdateQuickKeys(newKeys) },
            onResetDefault = { onResetQuickKeys() },
            onDismiss = { showCustomizeDialog = false }
        )
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CardDark,
            contentColor = WhitePrimary
        ),
        modifier = Modifier.height(34.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

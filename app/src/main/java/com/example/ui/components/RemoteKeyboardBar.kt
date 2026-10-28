package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary

@Composable
fun RemoteKeyboardBar(
    isOpen: Boolean,
    inputText: String,
    focusRequester: FocusRequester,
    onInputTextChange: (String) -> Unit,
    onSendText: (String) -> Unit,
    onKeyPress: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

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
                // Header with quick key actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpecialKeyButton(label = "ESC", onClick = { onKeyPress("ESC") })
                    SpecialKeyButton(label = "TAB", onClick = { onKeyPress("TAB") })
                    SpecialKeyButton(label = "SPACE", onClick = { onKeyPress("SPACE") })
                    SpecialKeyButton(label = "BKSP", onClick = { onKeyPress("BACKSPACE") })
                    SpecialKeyButton(label = "ENTER", onClick = { onKeyPress("ENTER") })
                    SpecialKeyButton(label = "▲", onClick = { onKeyPress("UP") })
                    SpecialKeyButton(label = "▼", onClick = { onKeyPress("DOWN") })
                    SpecialKeyButton(label = "◄", onClick = { onKeyPress("LEFT") })
                    SpecialKeyButton(label = "►", onClick = { onKeyPress("RIGHT") })
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input field + Send & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = { Text("Type to send text remotely...", fontSize = 13.sp, color = GrayMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhitePrimary,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = WhitePrimary,
                            unfocusedTextColor = WhitePrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                onSendText(inputText)
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("remote_text_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendText(inputText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhitePrimary,
                            contentColor = TrueBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("send_text_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send Text", tint = TrueBlack)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onClose()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Keyboard", tint = GrayMuted)
                    }
                }
            }
        }
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

package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.util.DesktopScriptGenerator

@Composable
fun DesktopScriptDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val scripts = listOf(
        "Windows (Zero Install)" to DesktopScriptGenerator.windowsPowerShellScript,
        "Python 3 (All OS)" to DesktopScriptGenerator.pythonReceiverScript
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Desktop Server",
                            tint = WhitePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Laptop Companion Server",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Windows, macOS, Linux • Zero data leaves your machine",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GrayMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Steps summary
                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "FAST SETUP GUIDE:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        StepItem(num = "1", text = "On Windows: Press Win+X -> Terminal/PowerShell (No installation needed!).")
                        StepItem(num = "2", text = "Paste the script below into PowerShell and press Enter.")
                        StepItem(num = "3", text = "In the app, enter your laptop's IP and tap Set & Connect!")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = WhitePrimary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = WhitePrimary
                        )
                    }
                ) {
                    scripts.forEachIndexed { index, (title, _) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) WhitePrimary else GrayMuted
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Script Code Box
                val currentScript = scripts[selectedTab].second
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrueBlack)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = currentScript,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = GraySecondary,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Copy Script
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AirGlide Server Script", currentScript)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhitePrimary,
                        contentColor = TrueBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_script_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Script",
                        tint = TrueBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy Script to Clipboard",
                        fontWeight = FontWeight.Bold,
                        color = TrueBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(num: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = ElevatedSurface,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = num,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = WhitePrimary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = WhitePrimary
        )
    }
}

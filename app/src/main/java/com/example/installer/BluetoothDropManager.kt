package com.example.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles beaming the Windows desktop receiver file to the laptop over Bluetooth (OPP / Object Push Profile)
 * or Android Quick Share / Nearby Share.
 */
object BluetoothDropManager {
    private const val TAG = "BluetoothDrop"

    /**
     * Extracts the receiver .exe and launches the Bluetooth transfer intent.
     * Triggers Windows' built-in Bluetooth File Transfer Wizard (fsquirt.exe) on the laptop.
     */
    suspend fun beamReceiverViaBluetooth(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val exeFile = AssetInstallerManager.getReceiverExeFile(context)
            if (!exeFile.exists() || exeFile.length() == 0L) {
                Log.e(TAG, "Receiver .exe does not exist for Bluetooth share")
                return@withContext false
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exeFile
            )

            withContext(Dispatchers.Main) {
                // Try direct Bluetooth OPP first
                val btIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.microsoft.portable-executable"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    setPackage("com.android.bluetooth")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val resolveInfo = context.packageManager.queryIntentActivities(btIntent, 0)
                if (resolveInfo.isNotEmpty()) {
                    context.startActivity(btIntent)
                } else {
                    // Fallback to system share chooser (Bluetooth / Quick Share / Nearby)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.microsoft.portable-executable"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Beam AirGlide to Laptop").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth beam failed: ${e.message}", e)
            false
        }
    }

    /**
     * Opens system share chooser with the receiver executable.
     */
    suspend fun shareReceiverGeneric(context: Context) = withContext(Dispatchers.IO) {
        try {
            val exeFile = AssetInstallerManager.getReceiverExeFile(context)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exeFile
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.microsoft.portable-executable"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "Share AirGlide to Laptop").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generic share failed: ${e.message}", e)
        }
    }
}

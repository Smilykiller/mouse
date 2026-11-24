package com.example.installer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
     * Extracts the receiver .exe and launches the Bluetooth transfer intent targeting a specific device.
     * Triggers Windows' built-in Bluetooth File Transfer Wizard (fsquirt.exe) on the laptop.
     */
    @SuppressLint("MissingPermission")
    suspend fun beamReceiverViaBluetooth(context: Context, targetDeviceAddress: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            // Android Bluetooth OPP and Windows Bluetooth Wizard frequently block raw .exe files.
            // Using the .zip package ensures the transfer succeeds across all Android & Windows versions.
            val zipFile = AssetInstallerManager.getReceiverZipFile(context)
            val fileToSend = if (zipFile.exists() && zipFile.length() > 0L) zipFile else AssetInstallerManager.getReceiverExeFile(context)
            if (!fileToSend.exists() || fileToSend.length() == 0L) {
                Log.e(TAG, "Receiver archive does not exist for Bluetooth share")
                return@withContext false
            }

            val mimeType = if (fileToSend.name.endsWith(".zip", ignoreCase = true)) "application/zip" else "application/vnd.microsoft.portable-executable"

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                fileToSend
            )

            withContext(Dispatchers.Main) {
                val btAdapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
                val targetDevice: BluetoothDevice? = if (!targetDeviceAddress.isNullOrBlank()) {
                    try { btAdapter?.getRemoteDevice(targetDeviceAddress) } catch (_: Exception) { null }
                } else null

                // Try direct Bluetooth OPP with targeted device
                val btIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    if (targetDevice != null) {
                        putExtra(BluetoothDevice.EXTRA_DEVICE, targetDevice)
                        putExtra("android.bluetooth.device.extra.DEVICE", targetDevice)
                    }
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
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        if (targetDevice != null) {
                            putExtra(BluetoothDevice.EXTRA_DEVICE, targetDevice)
                            putExtra("android.bluetooth.device.extra.DEVICE", targetDevice)
                        }
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val targetName = targetDevice?.name ?: "Laptop"
                    val chooser = Intent.createChooser(shareIntent, "Beam AirGlide to $targetName").apply {
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
     * Opens system share chooser with the receiver archive.
     */
    suspend fun shareReceiverGeneric(context: Context) = withContext(Dispatchers.IO) {
        try {
            val zipFile = AssetInstallerManager.getReceiverZipFile(context)
            val fileToSend = if (zipFile.exists() && zipFile.length() > 0L) zipFile else AssetInstallerManager.getReceiverExeFile(context)
            val mimeType = if (fileToSend.name.endsWith(".zip", ignoreCase = true)) "application/zip" else "application/vnd.microsoft.portable-executable"

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                fileToSend
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
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

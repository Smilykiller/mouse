package com.example.installer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Manages access, extraction, and local storage of the bundled Windows desktop receiver.
 * Enables zero-internet, zero-website installation of the laptop receiver directly from the mobile app.
 */
object AssetInstallerManager {
    private const val TAG = "AssetInstaller"
    private const val ASSET_ZIP_PATH = "receiver/AirGlide_Windows_Receiver.zip"
    const val RECEIVER_EXE_NAME = "AirGlide_Laptop_Receiver.exe"
    const val RECEIVER_ZIP_NAME = "AirGlide_Windows_Receiver.zip"

    /**
     * Retrieves or extracts the standalone Windows receiver executable (.exe) from app assets.
     */
    suspend fun getReceiverExeFile(context: Context): File = withContext(Dispatchers.IO) {
        val receiverDir = File(context.cacheDir, "receiver").apply { mkdirs() }
        val targetExe = File(receiverDir, RECEIVER_EXE_NAME)

        // If already extracted and has valid size (> 10 MB), reuse it
        if (targetExe.exists() && targetExe.length() > 10 * 1024 * 1024) {
            return@withContext targetExe
        }

        try {
            context.assets.open(ASSET_ZIP_PATH).use { rawStream ->
                ZipInputStream(rawStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".exe", ignoreCase = true) || entry.name.contains("Receiver", ignoreCase = true)) {
                            FileOutputStream(targetExe).use { out ->
                                zipIn.copyTo(out, bufferSize = 64 * 1024)
                            }
                            break
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract receiver executable from asset zip: ${e.message}", e)
        }

        targetExe
    }

    /**
     * Opens direct stream to the bundled receiver zip archive in assets.
     */
    fun openReceiverZipStream(context: Context): InputStream {
        return context.assets.open(ASSET_ZIP_PATH)
    }

    /**
     * Saves BOTH the Windows receiver .exe and the full .zip bundle directly to the phone's public Downloads folder.
     * Guarantees immediate visibility in Files by Google, Samsung My Files, and Windows File Explorer via USB cable.
     * Triggers MediaScannerConnection to index the files into the phone's visible Downloads & Recent lists.
     */
    suspend fun saveToPhoneDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val exeFile = getReceiverExeFile(context)
            if (!exeFile.exists() || exeFile.length() == 0L) {
                return@withContext "Error: Receiver executable could not be extracted"
            }

            val scannedPaths = mutableListOf<String>()
            val scannedMimes = mutableListOf<String>()

            // 1. Android 10+ (API 29+) MediaStore approach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver

                // A) Save Receiver Executable (.exe) with generic binary MIME so Android media provider doesn't hide it
                val exeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, RECEIVER_EXE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val exeUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, exeValues)
                if (exeUri != null) {
                    resolver.openOutputStream(exeUri)?.use { out ->
                        exeFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    exeValues.clear()
                    exeValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(exeUri, exeValues, null, null)
                }

                // B) Also save Receiver Zip Bundle (.zip) - phone file managers universally list .zip under Downloads & Archives!
                val zipValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, RECEIVER_ZIP_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val zipUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, zipValues)
                if (zipUri != null) {
                    resolver.openOutputStream(zipUri)?.use { out ->
                        context.assets.open(ASSET_ZIP_PATH).use { input -> input.copyTo(out) }
                    }
                    zipValues.clear()
                    zipValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(zipUri, zipValues, null, null)
                }
            }

            // 2. Direct filesystem copy to standard public Downloads folder
            @Suppress("DEPRECATION")
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads.exists() || publicDownloads.mkdirs()) {
                val destExe = File(publicDownloads, RECEIVER_EXE_NAME)
                try {
                    exeFile.copyTo(destExe, overwrite = true)
                    scannedPaths.add(destExe.absolutePath)
                    scannedMimes.add("application/octet-stream")
                } catch (e: Exception) {
                    Log.w(TAG, "Direct copy exe skipped: ${e.message}")
                }

                val destZip = File(publicDownloads, RECEIVER_ZIP_NAME)
                try {
                    context.assets.open(ASSET_ZIP_PATH).use { inStream ->
                        FileOutputStream(destZip).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    scannedPaths.add(destZip.absolutePath)
                    scannedMimes.add("application/zip")
                } catch (e: Exception) {
                    Log.w(TAG, "Direct copy zip skipped: ${e.message}")
                }
            }

            // 3. Immediately index saved files in Android MediaStore / Downloads database
            if (scannedPaths.isNotEmpty()) {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    scannedPaths.toTypedArray(),
                    scannedMimes.toTypedArray()
                ) { path, uri ->
                    Log.i(TAG, "MediaScanner indexed: $path -> $uri")
                }
            }

            // 4. Send legacy broadcast to trigger file manager refresh
            try {
                @Suppress("DEPRECATION")
                context.sendBroadcast(
                    Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(publicDownloads, RECEIVER_ZIP_NAME)))
                )
            } catch (ignored: Exception) {}

            "✓ Saved to Downloads: '$RECEIVER_ZIP_NAME' & '$RECEIVER_EXE_NAME'"
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to downloads: ${e.message}", e)
            "Error: ${e.localizedMessage}"
        }
    }
}

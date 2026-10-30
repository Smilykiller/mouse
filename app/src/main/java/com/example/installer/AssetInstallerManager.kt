package com.example.installer

import android.content.ContentValues
import android.content.Context
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
     * Saves the Windows receiver .exe to the phone's public Downloads folder.
     * Accessible immediately from PC File Explorer when connected via USB charging cable.
     */
    suspend fun saveToPhoneDownloads(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val exeFile = getReceiverExeFile(context)
            if (!exeFile.exists() || exeFile.length() == 0L) {
                return@withContext "Error: Receiver executable could not be extracted"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, RECEIVER_EXE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.microsoft.portable-executable")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AirGlide")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext "Error: Failed to create download record"

                resolver.openOutputStream(uri)?.use { outStream ->
                    exeFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                "Saved to Phone: Downloads/AirGlide/$RECEIVER_EXE_NAME"
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "AirGlide").apply { mkdirs() }
                val destFile = File(targetDir, RECEIVER_EXE_NAME)
                exeFile.copyTo(destFile, overwrite = true)
                "Saved to ${destFile.absolutePath}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to downloads: ${e.message}", e)
            "Error: ${e.localizedMessage}"
        }
    }
}

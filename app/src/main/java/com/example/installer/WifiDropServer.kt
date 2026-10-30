package com.example.installer

import android.content.Context
import android.util.Log
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * Built-in lightweight micro-HTTP server running on the phone.
 * Allows laptops to download the AirGlide desktop receiver directly over local Wi-Fi / hotspot
 * without internet access, third-party software, or visiting external websites.
 */
class WifiDropServer(
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideWifiDrop"

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _downloadCount = MutableStateFlow(0)
    val downloadCount: StateFlow<Int> = _downloadCount.asStateFlow()

    private val _lastDownloadStatus = MutableStateFlow<String?>(null)
    val lastDownloadStatus: StateFlow<String?> = _lastDownloadStatus.asStateFlow()

    fun start(context: Context, preferredPort: Int = 8080) {
        if (_isRunning.value) return

        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            val localIp = NetworkUtils.getLocalIpAddress()
            var port = preferredPort
            var ss: ServerSocket? = null

            // Try preferred port, fallback if in use
            for (offset in 0..5) {
                try {
                    ss = ServerSocket(preferredPort + offset)
                    port = preferredPort + offset
                    break
                } catch (_: Exception) { }
            }

            if (ss == null) {
                Log.e(tag, "Failed to bind WifiDrop server socket")
                _lastDownloadStatus.value = "Error: Could not bind local port"
                return@launch
            }

            serverSocket = ss
            _isRunning.value = true
            _serverUrl.value = "http://$localIp:$port"
            _lastDownloadStatus.value = "Ready! Open http://$localIp:$port on laptop"

            Log.i(tag, "WifiDrop server listening on ${_serverUrl.value}")

            try {
                while (isActive && !ss.isClosed) {
                    val clientSocket = try {
                        ss.accept()
                    } catch (_: Exception) {
                        break
                    }
                    scope.launch(Dispatchers.IO) {
                        handleClient(context, clientSocket)
                    }
                }
            } finally {
                stop()
            }
        }
    }

    private suspend fun handleClient(context: Context, socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine() ?: return@withContext

            val parts = firstLine.split(" ")
            if (parts.size < 2) return@withContext
            val path = parts[1].split("?")[0]

            val out = socket.getOutputStream()

            when {
                path == "/download/exe" -> {
                    serveExeDownload(context, out)
                }
                path == "/download/zip" -> {
                    serveZipDownload(context, out)
                }
                else -> {
                    serveLandingPage(out)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Client connection error: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun serveExeDownload(context: Context, out: OutputStream) {
        _lastDownloadStatus.value = "Preparing executable..."
        val exeFile = AssetInstallerManager.getReceiverExeFile(context)

        if (!exeFile.exists() || exeFile.length() == 0L) {
            val response = "HTTP/1.1 500 Internal Error\r\nContent-Length: 26\r\n\r\nError extracting receiver"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
            return
        }

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/vnd.microsoft.portable-executable\r\n" +
                "Content-Disposition: attachment; filename=\"${AssetInstallerManager.RECEIVER_EXE_NAME}\"\r\n" +
                "Content-Length: ${exeFile.length()}\r\n" +
                "Connection: close\r\n" +
                "\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.flush()

        exeFile.inputStream().use { fileIn ->
            val buf = ByteArray(64 * 1024)
            var bytesRead: Int
            while (fileIn.read(buf).also { bytesRead = it } != -1) {
                out.write(buf, 0, bytesRead)
            }
        }
        out.flush()

        _downloadCount.value += 1
        _lastDownloadStatus.value = "✓ AirGlide downloaded to laptop! Launch it to connect."
    }

    private fun serveZipDownload(context: Context, out: OutputStream) {
        val assetStream = AssetInstallerManager.openReceiverZipStream(context)
        val availableBytes = assetStream.available().toLong()

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/zip\r\n" +
                "Content-Disposition: attachment; filename=\"${AssetInstallerManager.RECEIVER_ZIP_NAME}\"\r\n" +
                (if (availableBytes > 0) "Content-Length: $availableBytes\r\n" else "") +
                "Connection: close\r\n" +
                "\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.flush()

        assetStream.use { inStream ->
            val buf = ByteArray(64 * 1024)
            var bytesRead: Int
            while (inStream.read(buf).also { bytesRead = it } != -1) {
                out.write(buf, 0, bytesRead)
            }
        }
        out.flush()

        _downloadCount.value += 1
        _lastDownloadStatus.value = "✓ AirGlide archive downloaded to laptop! Extract and run."
    }

    private fun serveLandingPage(out: OutputStream) {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AirGlide Remote for Windows - Direct Installer</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
        body { background: #0b0e14; color: #f1f5f9; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
        .card { background: #131722; border: 1px solid #1e2638; border-radius: 20px; padding: 36px 32px; max-width: 520px; width: 100%; box-shadow: 0 20px 40px rgba(0,0,0,0.6); text-align: center; }
        .badge { display: inline-block; background: rgba(6, 182, 212, 0.15); color: #38bdf8; border: 1px solid rgba(6, 182, 212, 0.3); font-size: 11px; font-weight: 700; letter-spacing: 1px; padding: 5px 12px; border-radius: 20px; margin-bottom: 18px; text-transform: uppercase; }
        h1 { font-size: 26px; font-weight: 800; margin-bottom: 8px; color: #ffffff; letter-spacing: -0.5px; }
        h1 span { color: #06b6d4; }
        p.subtitle { color: #94a3b8; font-size: 14px; margin-bottom: 28px; line-height: 1.5; }
        .btn-primary { display: block; background: linear-gradient(135deg, #0284c7 0%, #06b6d4 100%); color: #ffffff; font-size: 15px; font-weight: 700; text-decoration: none; padding: 16px 20px; border-radius: 12px; box-shadow: 0 4px 15px rgba(6, 182, 212, 0.35); transition: transform 0.15s, box-shadow 0.15s; margin-bottom: 12px; }
        .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(6, 182, 212, 0.5); }
        .btn-secondary { display: block; background: #1e2433; color: #cbd5e1; font-size: 13px; font-weight: 600; text-decoration: none; padding: 12px 16px; border-radius: 10px; border: 1px solid #334155; transition: background 0.15s; margin-bottom: 26px; }
        .btn-secondary:hover { background: #283144; color: #ffffff; }
        .steps { text-align: left; background: #0f121a; border-radius: 12px; padding: 18px; border: 1px solid #1a202c; }
        .step { display: flex; align-items: flex-start; margin-bottom: 12px; }
        .step:last-child { margin-bottom: 0; }
        .step-num { background: #1e293b; color: #38bdf8; font-weight: 800; font-size: 12px; border-radius: 50%; width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; margin-right: 12px; flex-shrink: 0; margin-top: 1px; }
        .step-text { font-size: 13px; color: #cbd5e1; }
        .step-text b { color: #ffffff; }
        .footer { margin-top: 24px; font-size: 12px; color: #64748b; }
    </style>
</head>
<body>
    <div class="card">
        <div class="badge">Direct Phone-to-Laptop Drop</div>
        <h1>AirGlide <span>Remote</span></h1>
        <p class="subtitle">Downloaded directly from your phone over local Wi-Fi. Zero internet access or web search required.</p>
        
        <a href="/download/exe" class="btn-primary">⚡ Download AirGlide for Windows (.exe)</a>
        <a href="/download/zip" class="btn-secondary">📦 Download Standalone Archive (.zip)</a>

        <div class="steps">
            <div class="step">
                <div class="step-num">1</div>
                <div class="step-text">Click the <b>Download</b> button above to save to your laptop.</div>
            </div>
            <div class="step">
                <div class="step-num">2</div>
                <div class="step-text">Double-click <b>AirGlide_Laptop_Receiver.exe</b> to launch.</div>
            </div>
            <div class="step">
                <div class="step-num">3</div>
                <div class="step-text">Your phone links automatically via Wi-Fi or Bluetooth!</div>
            </div>
        </div>

        <div class="footer">AirGlide • Ultra-low latency wireless trackpad & keyboard</div>
    </div>
</body>
</html>
        """.trimIndent()

        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${html.toByteArray(Charsets.UTF_8).size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"

        out.write((header + html).toByteArray(Charsets.UTF_8))
        out.flush()
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        _isRunning.value = false
        _serverUrl.value = ""
        serverJob?.cancel()
        serverJob = null
    }
}

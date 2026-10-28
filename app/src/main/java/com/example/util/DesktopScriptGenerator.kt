package com.example.util

object DesktopScriptGenerator {

    val windowsPowerShellScript: String = """
# ==============================================================================
# AirGlide Windows Native Receiver (PowerShell)
# ZERO INSTALLATION REQUIRED - Built into every Windows 10 & 11 machine!
# Includes Auto-Discovery Beacon (Mobile app detects this laptop automatically!)
# ==============================================================================
# How to run:
# 1. Press Win + X and click "Windows Terminal" or "PowerShell"
# 2. Paste this entire code and press Enter
# ==============================================================================

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class AirGlideNative {
    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, int dwData, int dwExtraInfo);
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo);
}
"@

§port = 5055
§udp = New-Object System.Net.Sockets.UdpClient(§port)
§endpoint = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, §port)

# Get local IP
§localIp = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { §_.InterfaceAlias -notmatch 'Loopback' -and §_.IPAddress -notmatch '^169\.254\.' } | Select-Object -First 1).IPAddress
if (-not §localIp) { §localIp = "127.0.0.1" }
§hostname = [System.Environment]::MachineName

Clear-Host
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " AirGlide Windows Native Receiver Running on Port 5055" -ForegroundColor Green
Write-Host " Laptop IP: §localIp" -ForegroundColor Yellow
Write-Host " Auto-Discovery: Active (Mobile app will detect this PC)" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor Cyan

§activeDev = §null
§lastBeacon = [System.DateTime]::MinValue

while (§true) {
    try {
        # Check and send periodic discovery beacon
        if (([System.DateTime]::Now - §lastBeacon).TotalSeconds -ge 1.5) {
            §beaconMsg = [System.Text.Encoding]::UTF8.GetBytes("AIRGLIDE_BEACON:§hostname:§localIp:§port")
            §broadcastEp = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Broadcast, §port)
            §udp.Send(§beaconMsg, §beaconMsg.Length, §broadcastEp) | Out-Null
            §lastBeacon = [System.DateTime]::Now
        }

        if (§udp.Available -eq 0) {
            Start-Sleep -Milliseconds 1
            continue
        }

        §bytes = §udp.Receive([ref]§endpoint)
        §msg = [System.Text.Encoding]::UTF8.GetString(§bytes).Trim()
        if ([string]::IsNullOrWhiteSpace(§msg)) { continue }

        # Discovery Ping
        if (§msg.StartsWith("AIRGLIDE_DISCOVERY_PING")) {
            §offerBytes = [System.Text.Encoding]::UTF8.GetBytes("AIRGLIDE_SERVER_OFFER:§hostname:§localIp:§port")
            §udp.Send(§offerBytes, §offerBytes.Length, §endpoint) | Out-Null
            Write-Host "[+] Mobile discovered this laptop from (§endpoint)" -ForegroundColor Green
            continue
        }

        §parts = §msg -split ':'
        if (§parts.Count -lt 2) { continue }
        §cmd = §parts[0]
        §dev = if (§parts.Count -ge 3) { §parts[§parts.Count - 1] } else { "unknown" }
        
        if (§null -eq §activeDev -and §dev -ne "unknown") {
            §activeDev = §dev
            Write-Host "[+] Connected Mobile Controller: §dev" -ForegroundColor Green
        }
        if (§activeDev -ne §null -and §dev -ne §activeDev -and §cmd -ne 'P') { continue }

        if (§cmd -eq 'M') {
            §coords = §parts[1] -split ','
            §dx = [Math]::Round([float]§coords[0])
            §dy = [Math]::Round([float]§coords[1])
            [AirGlideNative]::mouse_event(0x0001, §dx, §dy, 0, 0)
        }
        elseif (§cmd -eq 'C') {
            if (§parts[1] -eq 'L') {
                [AirGlideNative]::mouse_event(0x02, 0, 0, 0, 0)
                [AirGlideNative]::mouse_event(0x04, 0, 0, 0, 0)
            } elseif (§parts[1] -eq 'R') {
                [AirGlideNative]::mouse_event(0x08, 0, 0, 0, 0)
                [AirGlideNative]::mouse_event(0x10, 0, 0, 0, 0)
            } elseif (§parts[1] -eq 'M') {
                [AirGlideNative]::mouse_event(0x20, 0, 0, 0, 0)
                [AirGlideNative]::mouse_event(0x40, 0, 0, 0, 0)
            }
        }
        elseif (§cmd -eq 'D') {
            if (§parts[1] -eq 'R') { [AirGlideNative]::mouse_event(0x08, 0, 0, 0, 0) }
            elseif (§parts[1] -eq 'M') { [AirGlideNative]::mouse_event(0x20, 0, 0, 0, 0) }
            else { [AirGlideNative]::mouse_event(0x02, 0, 0, 0, 0) }
        }
        elseif (§cmd -eq 'U') {
            if (§parts[1] -eq 'R') { [AirGlideNative]::mouse_event(0x10, 0, 0, 0, 0) }
            elseif (§parts[1] -eq 'M') { [AirGlideNative]::mouse_event(0x40, 0, 0, 0, 0) }
            else { [AirGlideNative]::mouse_event(0x04, 0, 0, 0, 0) }
        }
        elseif (§cmd -eq 'S') {
            §dy = [Math]::Round([float]§parts[1]) * 120
            [AirGlideNative]::mouse_event(0x0800, 0, 0, §dy, 0)
        }
        elseif (§cmd -eq 'K') {
            §k = §parts[1].ToUpper()
            if (§k -eq "PAGE_UP" -or §k -eq "PAGEUP") { [System.Windows.Forms.SendKeys]::SendWait("{PGUP}") }
            elseif (§k -eq "PAGE_DOWN" -or §k -eq "PAGEDOWN") { [System.Windows.Forms.SendKeys]::SendWait("{PGDN}") }
            elseif (§k -eq "ENTER") { [System.Windows.Forms.SendKeys]::SendWait("{ENTER}") }
            elseif (§k -eq "BACKSPACE") { [System.Windows.Forms.SendKeys]::SendWait("{BACKSPACE}") }
            elseif (§k -eq "TAB") { [System.Windows.Forms.SendKeys]::SendWait("{TAB}") }
            elseif (§k -eq "SPACE") { [System.Windows.Forms.SendKeys]::SendWait(" ") }
            elseif (§k -eq "ESC") { [System.Windows.Forms.SendKeys]::SendWait("{ESC}") }
            elseif (§k -eq "UP") { [System.Windows.Forms.SendKeys]::SendWait("{UP}") }
            elseif (§k -eq "DOWN") { [System.Windows.Forms.SendKeys]::SendWait("{DOWN}") }
            elseif (§k -eq "LEFT") { [System.Windows.Forms.SendKeys]::SendWait("{LEFT}") }
            elseif (§k -eq "RIGHT") { [System.Windows.Forms.SendKeys]::SendWait("{RIGHT}") }
        }
        elseif (§cmd -eq 'T') {
            §txt = §parts[1] -replace '\\:', ':'
            [System.Windows.Forms.SendKeys]::SendWait(§txt)
        }
    } catch {}
}
""".trimIndent().replace('§', '$')

    val pythonReceiverScript: String = """
# ==============================================================================
# AirGlide Desktop Companion Server (Python 3)
# Ultra-Low Latency Unidirectional Mouse & Keyboard Receiver
# Compatible with: Windows, macOS, Linux
# ==============================================================================
# Run: python airglide_server.py
# ==============================================================================

import socket
import sys
import time
import threading
import platform

IS_WINDOWS = platform.system() == "Windows"

if IS_WINDOWS:
    import ctypes
    user32 = ctypes.windll.user32
else:
    try:
        import pyautogui
        pyautogui.FAILSAFE = False
        pyautogui.PAUSE = 0.0
    except ImportError:
        print("[AirGlide] Please install pyautogui: pip install pyautogui")
        sys.exit(1)

UDP_IP = "0.0.0.0"
UDP_PORT = 5055

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind((UDP_IP, UDP_PORT))

# Detect local IP
try:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    local_ip = s.getsockname()[0]
    s.close()
except:
    local_ip = "127.0.0.1"

hostname = socket.gethostname()

print("==================================================")
print(f"  AirGlide Desktop Server running on Port {UDP_PORT}")
print(f"  Laptop IP: {local_ip}")
print("  Auto-Discovery active: mobile connects automatically")
print("==================================================")

active_device_id = None
registered_devices = set()

# Beacon broadcast thread
def beacon_loop():
    b_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    b_sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    msg = f"AIRGLIDE_BEACON:{hostname}:{local_ip}:{UDP_PORT}".encode('utf-8')
    while True:
        try:
            b_sock.sendto(msg, ('255.255.255.255', UDP_PORT))
            b_sock.sendto(msg, ('255.255.255.255', 5056))
        except:
            pass
        time.sleep(1.5)

threading.Thread(target=beacon_loop, daemon=True).start()

while True:
    data, addr = sock.recvfrom(2048)
    msg = data.decode('utf-8', errors='ignore').strip()
    if not msg:
        continue

    # Discovery Ping
    if msg.startswith("AIRGLIDE_DISCOVERY_PING"):
        offer = f"AIRGLIDE_SERVER_OFFER:{hostname}:{local_ip}:{UDP_PORT}".encode('utf-8')
        sock.sendto(offer, addr)
        continue

    parts = msg.split(':')
    cmd = parts[0]
    dev_id = parts[-1] if len(parts) >= 3 else "unknown"

    if dev_id not in registered_devices and dev_id != "unknown":
        registered_devices.add(dev_id)
        print(f"\n[+] Controller Connected: {dev_id} ({addr[0]})")
        if active_device_id is None:
            active_device_id = dev_id
            print(f"[AirGlide] -> Locked active input to: {active_device_id}")

    if active_device_id is not None and dev_id != active_device_id and cmd != 'P':
        continue

    try:
        if cmd == 'M':
            coords = parts[1].split(',')
            dx, dy = float(coords[0]), float(coords[1])
            if IS_WINDOWS:
                user32.mouse_event(0x0001, int(round(dx)), int(round(dy)), 0, 0)
            else:
                pyautogui.moveRel(dx, dy)
        elif cmd == 'C':
            if IS_WINDOWS:
                if parts[1] == 'R':
                    user32.mouse_event(0x08, 0, 0, 0, 0); user32.mouse_event(0x10, 0, 0, 0, 0)
                elif parts[1] == 'M':
                    user32.mouse_event(0x20, 0, 0, 0, 0); user32.mouse_event(0x40, 0, 0, 0, 0)
                else:
                    user32.mouse_event(0x02, 0, 0, 0, 0); user32.mouse_event(0x04, 0, 0, 0, 0)
            else:
                btn = 'left' if parts[1] == 'L' else ('right' if parts[1] == 'R' else 'middle')
                pyautogui.click(button=btn)
        elif cmd == 'D':
            if IS_WINDOWS:
                user32.mouse_event(0x08 if parts[1] == 'R' else 0x02, 0, 0, 0, 0)
            else:
                pyautogui.mouseDown(button='left' if parts[1] == 'L' else 'right')
        elif cmd == 'U':
            if IS_WINDOWS:
                user32.mouse_event(0x10 if parts[1] == 'R' else 0x04, 0, 0, 0, 0)
            else:
                pyautogui.mouseUp(button='left' if parts[1] == 'L' else 'right')
        elif cmd == 'S':
            dy = float(parts[1])
            if IS_WINDOWS:
                user32.mouse_event(0x0800, 0, 0, int(round(dy * 120)), 0)
            else:
                pyautogui.scroll(int(round(dy * 20)))
        elif cmd == 'K':
            key = parts[1].lower()
            if IS_WINDOWS:
                vk_map = {'page_up': 0x21, 'pageup': 0x21, 'page_down': 0x22, 'pagedown': 0x22, 'enter': 0x0D, 'backspace': 0x08, 'tab': 0x09, 'space': 0x20, 'esc': 0x1B, 'up': 0x26, 'down': 0x28, 'left': 0x25, 'right': 0x27}
                if key in vk_map:
                    user32.keybd_event(vk_map[key], 0, 0, 0)
                    user32.keybd_event(vk_map[key], 0, 2, 0)
            else:
                pyautogui.press(key)
        elif cmd == 'T':
            text = parts[1].replace('\\:', ':')
            if IS_WINDOWS:
                for c in text:
                    user32.keybd_event(0, ord(c), 0x0004, 0)
                    user32.keybd_event(0, ord(c), 0x0004 | 0x0002, 0)
            else:
                pyautogui.write(text)
    except:
        pass
""".trimIndent()

    val nodeReceiverScript: String = """
// AirGlide Desktop Companion Server (Node.js)
const dgram = require('dgram');
const server = dgram.createSocket('udp4');

let activeDeviceId = null;
server.on('message', (msg, rinfo) => {
  const str = msg.toString().trim();
  if (str.startsWith('AIRGLIDE_DISCOVERY_PING')) {
    const offer = Buffer.from('AIRGLIDE_SERVER_OFFER:Laptop:' + rinfo.address + ':5055');
    server.send(offer, rinfo.port, rinfo.address);
    return;
  }
  const parts = str.split(':');
  const cmd = parts[0];
  const devId = parts.length >= 3 ? parts[parts.length - 1] : 'unknown';
  if (!activeDeviceId && devId !== 'unknown') activeDeviceId = devId;
  if (activeDeviceId && activeDeviceId !== devId && cmd !== 'P') return;
  console.log('[AirGlide]', cmd, parts[1]);
});
server.bind(5055, () => console.log('AirGlide Node Server listening on port 5055'));
""".trimIndent()
}

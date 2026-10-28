package com.example.util

/**
 * Copy-paste receiver scripts shown inside the app.
 *
 * NOTE: in the raw strings below the character § stands for the PowerShell dollar sign
 * (a real dollar would be treated as a Kotlin template). It is replaced at runtime.
 *
 * Protocol reminders (these are what the old scripts got wrong):
 *  - A receiver must NEVER send its beacon to its own listening port (5055). It would receive
 *    its own beacon and mistake "5055" for a phone, then ignore the real phone.
 *    Beacons go to the discovery port 5056 only.
 *  - Every phone packet ends with the phone's device id; anything starting with "AIRGLIDE_"
 *    is discovery traffic, never input.
 *  - A presence packet ("P:<id>:<name>") must be answered with "AIRGLIDE_ACK" so the phone can
 *    show a truthful "Live" status.
 */
object DesktopScriptGenerator {

    val windowsPowerShellScript: String = """
# ==============================================================================
# AirGlide Windows Receiver (PowerShell) - nothing to install
# 1. Press Win + X -> Terminal / PowerShell
# 2. Paste this whole script and press Enter
# 3. If Windows asks about the firewall, click "Allow access"
# ==============================================================================
Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class AirGlideNative {
    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, int dwData, int dwExtraInfo);
}
"@

§port = 5055
§udp = New-Object System.Net.Sockets.UdpClient(§port)
§beacon = New-Object System.Net.Sockets.UdpClient
§beacon.EnableBroadcast = §true
§endpoint = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)

# IP of the adapter that has the default gateway (ignores VirtualBox / WSL / VPN adapters)
§localIp = (Get-NetIPConfiguration | Where-Object { §_.IPv4DefaultGateway -ne §null -and §_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1).IPv4Address.IPAddress
if (-not §localIp) { §localIp = "127.0.0.1" }
§hostname = [System.Environment]::MachineName -replace ':', '-'

Clear-Host
Write-Host "AirGlide receiver running on UDP port §port" -ForegroundColor Green
Write-Host "Laptop IP: §localIp   (type this in the phone app if it is not found automatically)" -ForegroundColor Yellow

§activeDev = §null
§lastSeen = [DateTime]::MinValue
§lastBeacon = [DateTime]::MinValue
§valid = 'M','C','D','U','S','K','T','P'

while (§true) {
    try {
        if (([DateTime]::Now - §lastBeacon).TotalSeconds -ge 1.5) {
            §b = [Text.Encoding]::UTF8.GetBytes("AIRGLIDE_BEACON:§hostname:§localIp:§port")
            # discovery port 5056 ONLY - never our own port
            [void]§beacon.Send(§b, §b.Length, (New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Broadcast, 5056)))
            §lastBeacon = [DateTime]::Now
        }
        if (§udp.Available -eq 0) { Start-Sleep -Milliseconds 1; continue }

        §bytes = §udp.Receive([ref]§endpoint)
        §msg = [Text.Encoding]::UTF8.GetString(§bytes).Trim()
        if (-not §msg) { continue }

        if (§msg.StartsWith("AIRGLIDE_DISCOVERY_PING")) {
            §o = [Text.Encoding]::UTF8.GetBytes("AIRGLIDE_SERVER_OFFER:§hostname:§localIp:§port")
            [void]§udp.Send(§o, §o.Length, §endpoint)
            continue
        }
        if (§msg.StartsWith("AIRGLIDE_")) { continue }

        §parts = §msg -split ':'
        §cmd = §parts[0]
        if (§valid -notcontains §cmd -or §parts.Count -lt 3) { continue }
        §dev = if (§cmd -eq 'P') { §parts[1] } else { §parts[§parts.Count - 1] }

        §now = [DateTime]::Now
        if (§null -eq §activeDev -or (§now - §lastSeen).TotalSeconds -gt 10) { §activeDev = §dev; Write-Host "[+] Phone connected: §dev" -ForegroundColor Green }
        if (§dev -ne §activeDev) { continue }
        §lastSeen = §now

        if (§cmd -eq 'P') {
            §a = [Text.Encoding]::UTF8.GetBytes("AIRGLIDE_ACK:§hostname:§port")
            [void]§udp.Send(§a, §a.Length, §endpoint)
        }
        elseif (§cmd -eq 'M') {
            §c = §parts[1] -split ','
            [AirGlideNative]::mouse_event(0x0001, [int][Math]::Round([double]§c[0]), [int][Math]::Round([double]§c[1]), 0, 0)
        }
        elseif (§cmd -eq 'C') {
            if (§parts[1] -eq 'R') { [AirGlideNative]::mouse_event(0x08,0,0,0,0); [AirGlideNative]::mouse_event(0x10,0,0,0,0) }
            elseif (§parts[1] -eq 'M') { [AirGlideNative]::mouse_event(0x20,0,0,0,0); [AirGlideNative]::mouse_event(0x40,0,0,0,0) }
            else { [AirGlideNative]::mouse_event(0x02,0,0,0,0); [AirGlideNative]::mouse_event(0x04,0,0,0,0) }
        }
        elseif (§cmd -eq 'D') {
            if (§parts[1] -eq 'R') { [AirGlideNative]::mouse_event(0x08,0,0,0,0) } elseif (§parts[1] -eq 'M') { [AirGlideNative]::mouse_event(0x20,0,0,0,0) } else { [AirGlideNative]::mouse_event(0x02,0,0,0,0) }
        }
        elseif (§cmd -eq 'U') {
            if (§parts[1] -eq 'R') { [AirGlideNative]::mouse_event(0x10,0,0,0,0) } elseif (§parts[1] -eq 'M') { [AirGlideNative]::mouse_event(0x40,0,0,0,0) } else { [AirGlideNative]::mouse_event(0x04,0,0,0,0) }
        }
        elseif (§cmd -eq 'S') {
            [AirGlideNative]::mouse_event(0x0800, 0, 0, ([int][Math]::Round([double]§parts[1]) * 120), 0)
        }
        elseif (§cmd -eq 'K') {
            §map = @{ 'PAGE_UP'='{PGUP}'; 'PAGE_DOWN'='{PGDN}'; 'ENTER'='{ENTER}'; 'BACKSPACE'='{BACKSPACE}'; 'TAB'='{TAB}'; 'SPACE'=' '; 'ESC'='{ESC}'; 'UP'='{UP}'; 'DOWN'='{DOWN}'; 'LEFT'='{LEFT}'; 'RIGHT'='{RIGHT}' }
            §k = §parts[1].ToUpper()
            if (§map.ContainsKey(§k)) { [System.Windows.Forms.SendKeys]::SendWait(§map[§k]) }
        }
        elseif (§cmd -eq 'T') {
            # text may contain escaped colons; rebuild everything between the command and the device id
            §txt = (§parts[1..(§parts.Count - 2)] -join ':') -replace '\\:', ':' -replace '\\n', "`n" -replace '\\\\', '\'
            §txt = §txt -replace '([+^%~(){}\[\]])', '{§1}'
            [System.Windows.Forms.SendKeys]::SendWait(§txt)
        }
    } catch { }
}
""".trimIndent().replace('§', '$')

    val pythonReceiverScript: String = """
# AirGlide minimal receiver (Python 3). Windows: no extra packages. macOS/Linux: pip install pyautogui
# The full receiver with a window is in the project folder: desktop_receiver/airglide_receiver.py
import platform, socket, sys, threading, time

IS_WIN = platform.system() == "Windows"
PORT, DISCOVERY_PORT = 5055, 5056
VALID = set("MCDUSKTP")

if IS_WIN:
    import ctypes
    from ctypes import wintypes
    ULONG_PTR = ctypes.c_ulonglong if ctypes.sizeof(ctypes.c_void_p) == 8 else ctypes.c_ulong
    class MI(ctypes.Structure):
        _fields_ = [("dx", wintypes.LONG), ("dy", wintypes.LONG), ("data", wintypes.DWORD),
                    ("flags", wintypes.DWORD), ("time", wintypes.DWORD), ("extra", ULONG_PTR)]
    class KI(ctypes.Structure):
        _fields_ = [("vk", wintypes.WORD), ("scan", wintypes.WORD), ("flags", wintypes.DWORD),
                    ("time", wintypes.DWORD), ("extra", ULONG_PTR)]
    class HI(ctypes.Structure):
        _fields_ = [("msg", wintypes.DWORD), ("lo", wintypes.WORD), ("hi", wintypes.WORD)]
    class U(ctypes.Union):
        _fields_ = [("mi", MI), ("ki", KI), ("hi", HI)]
    class INPUT(ctypes.Structure):
        _fields_ = [("type", wintypes.DWORD), ("u", U)]
    user32 = ctypes.windll.user32
    user32.SendInput.argtypes = (wintypes.UINT, ctypes.POINTER(INPUT), ctypes.c_int)

    def send(*items):
        arr = (INPUT * len(items))(*items)
        user32.SendInput(len(items), arr, ctypes.sizeof(INPUT))

    def mouse(flags, dx=0, dy=0, data=0):
        i = INPUT(); i.type = 0; i.u.mi = MI(dx, dy, data & 0xFFFFFFFF, flags, 0, 0); return i

    def key(vk=0, scan=0, flags=0):
        i = INPUT(); i.type = 1; i.u.ki = KI(vk, scan, flags, 0, 0); return i

    BTN = {"L": (0x02, 0x04), "R": (0x08, 0x10), "M": (0x20, 0x40)}
    VK = {"ENTER": 0x0D, "BACKSPACE": 0x08, "TAB": 0x09, "SPACE": 0x20, "ESC": 0x1B, "UP": 0x26,
          "DOWN": 0x28, "LEFT": 0x25, "RIGHT": 0x27, "PAGE_UP": 0x21, "PAGE_DOWN": 0x22,
          "HOME": 0x24, "END": 0x23}
    EXT = {0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28}
else:
    try:
        import pyautogui
        pyautogui.FAILSAFE = False
        pyautogui.PAUSE = 0
    except ImportError:
        sys.exit("Please run: pip install pyautogui")

def unescape(s):
    out, i = [], 0
    while i < len(s):
        if s[i] == "\\" and i + 1 < len(s):
            out.append({"n": "\n"}.get(s[i + 1], s[i + 1])); i += 2
        else:
            out.append(s[i]); i += 1
    return "".join(out)

def act(cmd, payload):
    if IS_WIN:
        if cmd == "M":
            dx, dy = payload.split(","); send(mouse(0x0001, int(float(dx)), int(float(dy))))
        elif cmd == "C":
            d, u = BTN.get(payload, BTN["L"]); send(mouse(d), mouse(u))
        elif cmd == "D":
            send(mouse(BTN.get(payload, BTN["L"])[0]))
        elif cmd == "U":
            send(mouse(BTN.get(payload, BTN["L"])[1]))
        elif cmd == "S":
            send(mouse(0x0800, data=int(round(float(payload))) * 120))
        elif cmd == "K":
            vk = VK.get(payload.upper())
            if vk:
                f = 0x0001 if vk in EXT else 0
                send(key(vk, 0, f), key(vk, 0, f | 0x0002))
        elif cmd == "T":
            for ch in unescape(payload):
                if ch == "\n": send(key(0x0D), key(0x0D, 0, 2)); continue
                for unit in memoryview(ch.encode("utf-16-le")).cast("H"):
                    send(key(0, unit, 0x0004), key(0, unit, 0x0006))
    else:
        if cmd == "M":
            dx, dy = payload.split(","); pyautogui.moveRel(float(dx), float(dy))
        elif cmd == "C":
            pyautogui.click(button={"L": "left", "R": "right", "M": "middle"}.get(payload, "left"))
        elif cmd == "D":
            pyautogui.mouseDown(button="right" if payload == "R" else "left")
        elif cmd == "U":
            pyautogui.mouseUp(button="right" if payload == "R" else "left")
        elif cmd == "S":
            pyautogui.scroll(int(round(float(payload) * 20)))
        elif cmd == "K":
            pyautogui.press({"page_up": "pageup", "page_down": "pagedown", "bksp": "backspace"}.get(payload.lower(), payload.lower()))
        elif cmd == "T":
            pyautogui.write(unescape(payload))

def local_ip(peer=None):
    for target in ([peer] if peer else []) + ["8.8.8.8", "192.168.0.1", "192.168.43.1", "10.0.0.1", "172.20.10.1"]:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect((target, 9))
            ip = s.getsockname()[0]; s.close()
            if not ip.startswith("127."): return ip
        except OSError:
            pass
    return "127.0.0.1"

host = socket.gethostname().replace(":", "-")
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", PORT))

def beacon():
    b = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    b.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    while True:
        ip = local_ip()
        msg = ("AIRGLIDE_BEACON:%s:%s:%d" % (host, ip, PORT)).encode()
        for dest in ("255.255.255.255", ip.rsplit(".", 1)[0] + ".255"):
            try: b.sendto(msg, (dest, DISCOVERY_PORT))   # discovery port only, never our own port
            except OSError: pass
        time.sleep(1.5)

threading.Thread(target=beacon, daemon=True).start()
print("AirGlide receiver on UDP %d - laptop IP %s" % (PORT, local_ip()))

active, last_seen = None, 0.0
while True:
    data, addr = sock.recvfrom(2048)
    msg = data.decode("utf-8", "ignore").strip()
    if msg.startswith("AIRGLIDE_DISCOVERY_PING"):
        sock.sendto(("AIRGLIDE_SERVER_OFFER:%s:%s:%d" % (host, local_ip(addr[0]), PORT)).encode(), addr)
        continue
    if msg.startswith("AIRGLIDE_") or ":" not in msg:
        continue
    cmd, rest = msg.split(":", 1)
    if cmd not in VALID:
        continue
    if cmd == "P":
        dev = rest.split(":", 1)[0]; payload = ""
    else:
        payload, _, dev = rest.rpartition(":")
        if not dev: continue
    now = time.time()
    if active is None or now - last_seen > 10:
        active = dev; print("Phone connected:", dev, addr[0])
    if dev != active:
        continue
    last_seen = now
    if cmd == "P":
        sock.sendto(("AIRGLIDE_ACK:%s:%d" % (host, PORT)).encode(), addr)
        continue
    try:
        act(cmd, payload)
    except Exception as e:
        print("error:", e)
""".trimIndent()
}

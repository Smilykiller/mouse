# AirGlide – phone as a wireless mouse (fixed version)

## What was wrong (and is fixed here)

| # | Problem | Effect |
|---|---------|--------|
| 1 | **Every receiver broadcast its beacon to its own port 5055, received it, and treated "5055" as the phone's ID – then ignored the real phone.** | Phone says "Streaming", packet counter rises, **cursor never moves** |
| 2 | Laptop IP = first adapter found (often VirtualBox/WSL/VPN); phone trusted that advertised IP | Phone connected to an unreachable address |
| 3 | Beacon sent only via `255.255.255.255` | Reached only one network adapter |
| 4 | "Live/Connected" status was guessed from beacons only | Misleading status. Now the laptop answers each heartbeat (`AIRGLIDE_ACK`) |
| 5 | Auto-connect overwrote an IP you typed manually | Fixed – manual choice is kept |
| 6 | Keys (Enter/Backspace/arrows) used `SendKeys` from a background thread; text escaping mismatch; `keybd_event` unicode | Special keys/typing failed. Now `SendInput` everywhere |
| 7 | Scroll strip far too fast (×0.25), two-finger scroll advertised but missing | Fixed (×0.05, two-finger scroll + long-press = right click) |
| 8 | Android build blockers: missing `ic_airglide_logo.jpg`, missing `android.useAndroidX=true`, unused Firebase/Room/Moshi/Retrofit, bleeding-edge AGP 9 / Gradle 9 | Removed/pinned to stable AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21 |
| 9 | Node.js "receiver" only printed to the console | Removed |

## 1. Laptop (Windows)

**Easiest – Python** (no compiler):
1. Install Python 3 from python.org (tick *Add to PATH*).
2. Double-click `Start_Laptop_Receiver.bat` (or `python desktop_receiver\airglide_receiver.py`).
3. If Windows asks about the firewall → **Allow** (Private + Public). If the phone still can't connect, run `Fix_Firewall.bat`.

macOS/Linux: `pip install pynput`, then `python3 desktop_receiver/airglide_receiver.py`.

**Native exe (optional):** install the .NET 8 SDK, then in `desktop_receiver`: `dotnet publish -c Release -r win-x64 --self-contained -o publish`.

## 2. Phone (Android)
1. Android Studio → **File ▸ Open** → this folder (the one with `settings.gradle.kts`). Accept SDK/Gradle downloads, wait for sync.
2. Plug in the phone (USB debugging) → **Run ▸**.
3. Phone and laptop on the **same Wi-Fi** (or laptop on the phone's hotspot).

## 3. Reading the status pill
* **Live** (green) – laptop answered. Everything works.
* **No reply** (amber) – packets are sent but the laptop doesn't answer: receiver not running, wrong IP, firewall, or the router blocks device-to-device traffic ("AP/client isolation", common on guest/public Wi-Fi – use your phone's hotspot instead).
* **No Wi-Fi** (red) – phone has no local network.

Tip: if auto-discovery fails, type the IP shown in the receiver window into **Connect** in the app.

## Notes
* Only one phone controls the PC at a time (stops strangers on shared Wi-Fi). The lock frees after 10 s of silence, or press **Release phone**.
* Traffic is unencrypted UDP on your LAN – use trusted networks.
* Windows cannot let a normal program control elevated windows (Task Manager, UAC prompts) unless the receiver runs as Administrator.
* Python tests: `cd desktop_receiver && python -m unittest discover -s tests -v`.

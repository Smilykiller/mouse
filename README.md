# AirGlide - Ultra-Low Latency Wireless Remote & Touchpad

AirGlide transforms your Android phone or tablet into a sub-millisecond wireless mouse, adaptive touchpad, presentation remote, and remote keyboard for your laptop or desktop over local Wi-Fi or mobile hotspot.

---

## 🚀 Quick Start: Running on Laptop & Phone

### 1. Laptop Setup (Separate Desktop Version)
Navigate to the `desktop_receiver` folder:
```
d:\zentry_hub_projects\app_development\mouse\desktop_receiver\
```

* **Option A (Instant 1-Click Launch - Recommended):**
  * Double-click **`Start_AirGlide_Laptop.bat`** (or run `publish\AirGlideDesktop.exe`).
  * The modern dark-themed GUI will open, displaying your laptop's IP address (e.g. `192.168.1.34:5055`) and start broadcasting the **Auto-Discovery Beacon**.
* **Option B (Permanent Desktop Shortcut):**
  * Double-click **`Install_Laptop_App.bat`** to generate a desktop shortcut and configure firewall rules.
* **Option C (Cross-Platform Python GUI - macOS/Linux/Windows):**
  * Run `py airglide_desktop_gui.py` or `python airglide_desktop_gui.py`.

---

### 2. Mobile App Setup (Android)
* Open this root project folder in **Android Studio**.
* Connect your Android phone via USB/Wi-Fi or start an emulator.
* Click **Run (Shift + F10)**.
* **Auto-Discovery:** When the app opens, it automatically detects your running laptop receiver!
  * Tap **Connect** on the detected laptop badge.
  * You're connected! Swipe on the touchpad, use twin click buttons, or wave your phone in the air with the Gyro Air-Mouse.

---

## 🛠️ Key Bug Fixes & Architecture Improvements

1. **High-Frequency UDP Coroutine Thrashing Fixed:**
   * *Previous bug:* Launching 120 coroutines/sec on `Dispatchers.IO` for every pointer move caused packet reordering and cursor jitter.
   * *Fix:* Implemented a single sequential Kotlin `Channel<String>` worker loop ensuring strict FIFO delivery and 0ms latency.
2. **Auto-Discovery Beaconing (Zero-Configuration Pairing):**
   * *Previous limitation:* Users were forced to manually hunt down their laptop IPv4 address and type it into the phone.
   * *Fix:* The laptop desktop receiver continuously broadcasts `AIRGLIDE_BEACON`. The mobile app automatically discovers it and auto-connects with 1 tap.
3. **Scroll Wheel Delta Truncation Fixed:**
   * *Previous bug:* Slow drags on the scroll strip resulted in small fractional deltas (e.g. `0.4f`) that evaluated to `0.toInt()`, causing slow scrolls to be completely ignored.
   * *Fix:* Added an accumulator pipeline so fractional distances smoothly build into discrete scroll clicks without lost steps.
4. **Gyroscope Air-Mouse & Drift Elimination:**
   * *Previous bug:* Raw accelerometer had a 9.8 m/s² gravity vector that caused the cursor to slide endlessly off screen when tilted.
   * *Fix:* Switched to true angular velocity from `Sensor.TYPE_GYROSCOPE` (with resting deadzone and center calibration), matching commercial motion remotes.
5. **Drag-Lock Desynchronization Fixed:**
   * *Previous bug:* Clicking while Drag Lock was active broke Windows drag state without updating the UI state.
   * *Fix:* Synchronized mouse up/down triggers so tapping Left Click during Drag Lock smoothly releases the item.
6. **Physical Volume Rocker HUD Suppressed:**
   * *Previous bug:* Pressing volume buttons on some Android devices triggered the system volume slider dialog.
   * *Fix:* Intercepted both `onKeyDown` and `onKeyUp` returning `true` to cleanly suppress OS volume changes.
7. **Accurate Live Connection Status:**
   * *Previous bug:* Showed green "Connected" even when target laptop was off.
   * *Fix:* Green "Live Connected" only shows when live heartbeats/beacons are received from the laptop within the last 6 seconds.
8. **Built-in Debug Keystore Build Error Fixed:**
   * *Previous bug:* `app/build.gradle.kts` referenced a missing `debug.keystore`, failing local debug builds.
   * *Fix:* Added file existence guard to fall back safely to Android's default debug keystore.

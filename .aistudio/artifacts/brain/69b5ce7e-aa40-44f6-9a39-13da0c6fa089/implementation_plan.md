# AirGlide - Low-Latency Remote Mouse & Adaptive Touchpad

AirGlide transforms your Android phone or tablet into an ultra-low latency wireless mouse, scroll wheel, and remote keyboard for your laptop, desktop, or tablet over local Wi-Fi/hotspot and Bluetooth, with strict unidirectional data security and multi-device arbitration.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been synthesized from your answers and technical requirements:

- **Confirmed Dual Network Transports (UDP + Bluetooth)**: Primary ultra-low latency unidirectional UDP datagram streaming over local Wi-Fi/Hotspot (sub-millisecond latency, zero-handshake overhead) combined with Bluetooth HID/RFCOMM mode for offline pairing without a router.
- **Auto-Adaptive Dual Role (Mobile & Laptop/Tablet)**: The app detects window size classes: Compact screens default to **Mobile Controller Mode**, while Expanded screens (tablets, Chromebooks, Android desktop) default to **Host Receiver Mode** with multi-device arbitration. A top-bar role switcher allows manual toggling at any time.
- **All 3 Touchpad Layouts Included**: User-switchable layout engine featuring:
  1. *Split Layout* (Large trackpad + dedicated vertical scroll strip + twin large Left & Right click boxes + keyboard trigger).
  2. *Classic Laptop Layout* (Expansive touchpad surface with dual bottom click buttons and two-finger scroll).
  3. *Gesture Surface* (Full-screen minimalist gesture pad with tap-to-click, two-finger swipe scroll, and floating controls).
- **Multi-Device Arbitration on Host**: Host mode tracks all incoming device connections and features a **Device Selector** where the operator locks input to one active device, blocking rogue inputs or command collisions.
- **Unidirectional Air-Gap Security**: Control packets flow strictly Mobile &rarr; Laptop. No file system access, screen capture, or return data telemetry is received or accepted by the mobile client.
- **Bundled Desktop Receiver Helper**: Built-in tab that provides a one-click copyable/exportable zero-dependency Python script (`airglide_server.py`) and Node.js receiver using OS-native mouse/keyboard automation for Windows, macOS, and Linux.

---

## 1. Overview & Core Concept

### What It Does
AirGlide provides instantaneous cursor navigation, scrolling, click actions, and remote keyboard entry from your mobile device to any target computer or tablet on the same Wi-Fi network, hotspot, or Bluetooth link. When running on an Android tablet or desktop, it functions as the Receiver Host; when running on a phone, it functions as an ergonomic trackpad.

### Target Persona
- Users presenting slides or watching media from a couch or conference room without a physical mouse.
- Workstation operators needing a quick backup pointing device, scroll wheel, or remote keyboard.
- Multi-device users wanting to control a laptop, tablet, or media center seamlessly.

### Key Value & Privacy Mandate
- **Near-Zero Latency**: Direct UDP datagram packets prevent TCP head-of-line blocking and keep cursor movement fluid at 60–120Hz.
- **Strict Privacy**: Unidirectional broadcast of coordinate deltas; no telemetry, passwords, or personal data leave your local network.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Auto-Detection & Launch**:
   - On phones: Launches straight into the responsive Touchpad surface with a status pill ("Ready / Streaming on Port 5055").
   - On tablets/desktops: Launches into the Host Monitor showing paired/incoming client devices, an active device selector switch, and real-time cursor activity.
2. **Controller Interaction**:
   - **Trackpad Motion**: Smooth 1:1 or accelerated cursor tracking with customizable sensitivity.
   - **Click Buttons**: Large dual touch targets for Left Click and Right Click with crisp haptic vibrations (`Vibrator`).
   - **Scroll Area**: Dedicated swipe strip or two-finger gesture for buttery smooth vertical document/page scrolling.
   - **Keyboard Drawer**: Tap the keyboard icon to pop up the Android software keyboard and stream keystrokes directly.
3. **Desktop Server Setup**:
   - "Connect to Laptop" drawer shows the target IP input, automatic Wi-Fi hotspot IP detector, and a "Desktop Server" screen with a one-click copyable 30-line standalone Python/Node server script.

### Visual Identity & Theme
- **Theme**: *Cyber-Precision Minimalist Dark* (deep slate `#0F172A`, obsidian surface `#1E293B`, neon cyan accent `#06B6D4`, electric violet secondary `#8B5CF6`).
- **Typography**: Clean high-legibility geometric sans with clear spatial metrics (`sp` scaled) and mono styling for IP addresses and packet counters.
- **Tactile Feedback**: Subtle tactile haptic pulses on left/right click, visual glowing ripples on tap, and real-time latency ping indicators.
- **Edge-to-Edge**: Full screen bleed respecting status bar and navigation bar safe insets.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale | Alternatives Considered |
| :--- | :--- | :--- | :--- |
| **Network Protocol** | Unidirectional UDP Datagrams (`DatagramSocket`) | Zero latency, no handshake delay, immune to packet queuing/HOL delays. | TCP / WebSockets (high latency jitter under packet drops). |
| **Bluetooth Integration** | Bluetooth RFCOMM / BLE command profile | Enables remote mouse control when no Wi-Fi or hotspot is available. | Bluetooth HID Device profile (only supported on select OEM hardware). |
| **Host Arbitration** | Device UUID & IP active whitelist selector | Solves multi-device collisions; host explicitly locks active controller. | First-come-first-serve (causes cursor jumping when two phones touch). |
| **Permissions Strategy** | Progressive runtime prompts with Wi-Fi first | Wi-Fi UDP requires only install-time `INTERNET` and `ACCESS_NETWORK_STATE`; Bluetooth runtime permissions are only asked when Bluetooth mode is activated. | Demanding all Bluetooth permissions at startup. |
| **Local Persistence** | Jetpack DataStore / Room for device history | Fast, zero-overhead offline persistence for target IP addresses and sensitivity preferences. | Cloud database (unnecessary and violates unidirectional privacy). |

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AirGlide Architecture                           │
└────────────────────────────────────────────────────────────────────────┘

     ┌─────────────────────────────────────────────────────────────┐
     │                 Adaptive UI Layer (M3 Compose)              │
     │  ┌───────────────────────┐       ┌───────────────────────┐  │
     │  │ Phone: Controller View│       │ Tablet: Host Receiver │  │
     │  │  - Trackpad Surface   │       │  - Device Selector    │  │
     │  │  - Scroll Wheel Zone  │       │  - Active Lock Toggle │  │
     │  │  - Left / Right Click │       │  - Cursor Simulator   │  │
     │  │  - Remote Keyboard    │       │  - Connection Log     │  │
     │  └───────────┬───────────┘       └───────────▲───────────┘  │
     └──────────────┼───────────────────────────────┼──────────────┘
                    │                               │
     ┌──────────────▼───────────────────────────────┴──────────────┐
     │           Controller / Host State Manager (ViewModel)       │
     │  - Input Throttle & Delta Batcher (120 Hz)                  │
     │  - Target Host IP & Port Management                         │
     │  - Layout State (Split / Classic / Gesture)                 │
     │  - Unidirectional Command Encoder / Decoder                 │
     └──────────────┬───────────────────────────────▲──────────────┘
                    │                               │
     ┌──────────────▼───────────────────────────────┴──────────────┐
     │                  Transport Layer (Zero-Ack)                 │
     │  ┌─────────────────────────────┐ ┌───────────────────────┐  │
     │  │ UDP Datagram Socket Client  │ │ Bluetooth RFCOMM / BLE│  │
     │  └──────────────┬──────────────┘ └───────────┬───────────┘  │
     └─────────────────┼────────────────────────────┼──────────────┘
                       │ Local Network Packets      │
                       ▼                            ▼
            ┌──────────────────────────────────────────────┐
            │ Target Workstation / Laptop (Windows/Mac/Lin)│
            │  - airglide_server.py (Standalone Zero-Dep)  │
            │  - Receives `M:dx,dy`, `C:L`, `S:dy`, `K:key`│
            │  - Dispatches native mouse/keyboard events   │
            └──────────────────────────────────────────────┘
```

### Unidirectional Packet Protocol Specification

Compact binary-like text format designed for microsecond parsing:
- `M:<dx>,<dy>` &rarr; Relative cursor movement delta (e.g. `M:14,-6`)
- `C:<L|R|M>` &rarr; Mouse click event (Left, Right, Middle)
- `D:<L|R>` &rarr; Mouse button down (for drag-and-drop)
- `U:<L|R>` &rarr; Mouse button up
- `S:<dy>` &rarr; Vertical scroll delta (e.g. `S:-3` for scroll down, `S:3` for scroll up)
- `K:<char_or_action>` &rarr; Key press event (e.g. `K:ENTER`, `K:BACKSPACE`, `K:A`)
- `P:<device_id>:<device_name>` &rarr; Presence heartbeat to allow Host selector arbitration

### Required Permissions Analysis

1. **Local Wi-Fi & Hotspot (Primary Mode)**:
   - `android.permission.INTERNET` (Install-time normal permission)
   - `android.permission.ACCESS_NETWORK_STATE` (To check Wi-Fi connection and retrieve local IP)
   - `android.permission.ACCESS_WIFI_STATE` (To read current Wi-Fi SSID / Hotspot status)
   - `android.permission.CHANGE_WIFI_MULTICAST_STATE` (For automatic device discovery broadcasts)
2. **Bluetooth Mode (Optional Secondary Mode)**:
   - `android.permission.BLUETOOTH` & `android.permission.BLUETOOTH_ADMIN` (API &le; 30)
   - `android.permission.BLUETOOTH_SCAN` & `android.permission.BLUETOOTH_CONNECT` & `android.permission.BLUETOOTH_ADVERTISE` (API &ge; 31, requested only upon activating Bluetooth)
3. **Sensory & Power**:
   - `android.permission.VIBRATE` (Haptic feedback for mechanical click feel)
   - `android.permission.WAKE_LOCK` (Prevents screen and network sleep during active touchpad use)

---

## 5. Execution Steps for Phase 3

1. **Metadata & Strings Setup**: Update `metadata.json` and `strings.xml` with app branding "AirGlide".
2. **Permissions & Manifest**: Add network, Wi-Fi, Bluetooth, wake lock, and vibration permissions to `AndroidManifest.xml`.
3. **Network & Protocol Engine**:
   - Implement `UdpClient` for low-latency datagram transmission.
   - Implement `UdpHostServer` for in-app tablet/host receiver with multi-device arbitration.
   - Implement `BluetoothManager` for Bluetooth connectivity.
4. **Touch & Gesture Processing**:
   - Velocity tracking, delta smoothing, and acceleration math for natural cursor movement.
   - 3 switchable layouts: Split layout, Classic laptop layout, Gesture surface.
   - Large Left & Right click boxes with haptic pulses and drag-lock support.
   - Dedicated scroll area with direction inversion toggle.
   - Remote software keyboard input bar with support for special keys (Enter, Backspace, Tab, Space, Esc).
5. **Adaptive UI & Host Device Selector**:
   - Window size class detection with manual switch.
   - Host mode with connected device list, active device selector, live visual cursor pad, and packet log.
   - Built-in "Laptop Server Setup" screen displaying the Python/Node desktop script with quick copy, instructions, and QR connection info.
6. **Compile & Verification**: Run `compile_applet` and verify builds cleanly.

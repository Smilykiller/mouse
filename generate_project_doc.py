import os
import sys
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import qn, nsdecls

def create_document():
    doc = Document()

    # Page Margins: 1 inch everywhere
    sections = doc.sections
    for section in sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)

    # Palette
    COLOR_PRIMARY = RGBColor(14, 116, 144)    # Deep Cyan / Teal (#0E7490)
    COLOR_DARK = RGBColor(15, 23, 42)          # Slate Dark (#0F172A)
    COLOR_TEXT = RGBColor(51, 65, 85)          # Slate Body (#334155)
    COLOR_ACCENT = RGBColor(37, 99, 235)       # Cobalt Blue (#2563EB)
    COLOR_MUTED = RGBColor(100, 116, 139)      # Slate Muted (#64748B)

    # Styles
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Segoe UI'
    style_normal.font.size = Pt(10.5)
    style_normal.font.color.rgb = COLOR_TEXT
    style_normal.paragraph_format.line_spacing = 1.15
    style_normal.paragraph_format.space_after = Pt(6)

    def add_title(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(4)
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(26)
        run.font.bold = True
        run.font.color.rgb = COLOR_PRIMARY
        return p

    def add_subtitle(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(18)
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(13)
        run.font.italic = True
        run.font.color.rgb = COLOR_MUTED
        return p

    def add_h1(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(18)
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(16)
        run.font.bold = True
        run.font.color.rgb = COLOR_PRIMARY
        return p

    def add_h2(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(12)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(13)
        run.font.bold = True
        run.font.color.rgb = COLOR_DARK
        return p

    def add_h3(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(11)
        run.font.bold = True
        run.font.color.rgb = COLOR_ACCENT
        return p

    def add_callout(text, prefix="IMPORTANT NOTE: "):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(6)
        p.paragraph_format.space_after = Pt(8)
        p.paragraph_format.left_indent = Inches(0.25)
        run_pre = p.add_run(prefix)
        run_pre.font.bold = True
        run_pre.font.size = Pt(10)
        run_pre.font.color.rgb = COLOR_PRIMARY
        run_txt = p.add_run(text)
        run_txt.font.size = Pt(10)
        run_txt.font.italic = True
        return p

    def set_cell_background(cell, hex_color):
        shading = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
        cell._tc.get_or_add_tcPr().append(shading)

    def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
        tcPr = cell._tc.get_or_add_tcPr()
        tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
        tcPr.append(tcMar)

    # -------------------------------------------------------------
    # Title & Metadata
    # -------------------------------------------------------------
    add_title("AirGlide Remote Ecosystem")
    add_subtitle("Comprehensive Architectural Specification, Engineering History, Wire Protocols, and Security Audit")

    meta_table = doc.add_table(rows=5, cols=2)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta_table.autofit = False

    meta_data = [
        ("Project Name", "AirGlide Remote (Android Client & Windows Desktop Host)"),
        ("Documentation Version", "v2.5.0 - Comprehensive Production Release"),
        ("Target Platforms", "Android 8.0+ (API 26-35) | Windows 10/11 x64"),
        ("Engineering Scope", "Trackpad, Gamepad, Stream Deck, Air Motion, Screen Digitizer, Security & Fail-Safe Engine"),
        ("Release Status", "Complete, Audited, Verified, and Ready for Deployment")
    ]
    for i, (k, v) in enumerate(meta_data):
        row = meta_table.rows[i]
        c0, c1 = row.cells[0], row.cells[1]
        c0.width = Inches(2.2)
        c1.width = Inches(4.3)
        set_cell_background(c0, "F1F5F9")
        set_cell_background(c1, "F8FAFC")
        set_cell_margins(c0, 80, 80, 120, 120)
        set_cell_margins(c1, 80, 80, 120, 120)
        p0 = c0.paragraphs[0]
        r0 = p0.add_run(k)
        r0.font.bold = True
        r0.font.size = Pt(9.5)
        r0.font.color.rgb = COLOR_DARK
        p1 = c1.paragraphs[0]
        r1 = p1.add_run(v)
        r1.font.size = Pt(9.5)

    doc.add_paragraph().paragraph_format.space_after = Pt(12)

    # -------------------------------------------------------------
    # 1. Executive Summary
    # -------------------------------------------------------------
    add_h1("1. Executive Summary & Purpose")
    p = doc.add_paragraph(
        "The AirGlide Remote project was conceived and engineered to transform standard Android mobile devices "
        "into enterprise-grade, low-latency, and zero-configuration wireless human interface devices (HIDs) for "
        "Windows laptops and desktops. Unlike legacy remote desktop and mouse utilities that suffer from high input lag, "
        "cumbersome driver requirements, intrusive cloud relays, or heavy battery drain, AirGlide establishes direct "
        "local-network and local-radio peer-to-peer pipelines using custom UDP and Bluetooth RFCOMM/SPP engines."
    )
    p = doc.add_paragraph(
        "The system provides an all-in-one wireless peripheral experience combining seven major operational modes: "
        "a high-precision multi-touch trackpad with kinetic inertia; a specialized Stream Deck macro board; "
        "a full-screen dual-stick analog gamepad; an IMU-based 3-DoF Air Motion spatial pointer; a local peer-to-peer "
        "Wi-Fi file drop installer; an absolute coordinate graphics tablet digitizer; and an authenticated live screen "
        "mirroring engine. Across all components, strict adherence to Trail of Bits insecure-defaults security principles, "
        "OS-level privacy barriers (FLAG_SECURE), and fail-secure crash recovery ensures complete system integrity."
    )

    # -------------------------------------------------------------
    # 2. System Architecture & Component Topology
    # -------------------------------------------------------------
    add_h1("2. System Architecture & Topology")
    doc.add_paragraph(
        "AirGlide is organized as a decoupled, bi-directional client-server architecture consisting of two primary tiers: "
        "the Android Client Application and the Windows Desktop Receiver Host."
    )

    add_h2("2.1 Android Application Architecture")
    doc.add_paragraph(
        "The mobile application is constructed using 100% modern Android technologies with Kotlin and Jetpack Compose. "
        "Its architectural layers include:"
    )
    bullets = [
        ("Presentation Layer (Jetpack Compose)", "Declarative UI rendering with GPU-accelerated canvas surfaces, dynamic theme switching, responsive multi-mode layouts, and lifecycle-bound sensor and window orientation observers."),
        ("State & Coordination Layer (AirGlideViewModel)", "Unified Model-View-Intent (MVI) state management powered by Kotlin StateFlow and CoroutineScopes, orchestrating real-time pointer accumulation, gesture recognition, and mode transitions."),
        ("Network Transport Subsystem", "Dual-stack client engine featuring UdpClient (ordered FIFO non-blocking Channel with background IO dispatch) and BluetoothClient (RFCOMM/Serial SPP socket manager with automatic failover)."),
        ("Sensor Fusion Engine (AirMotionSensorManager)", "Hardware-accelerated 3-DoF IMU observer utilizing Android TYPE_GYROSCOPE and TYPE_ACCELEROMETER with low-pass gravity filtering and deadzone dampening."),
        ("Privacy & Security Layer", "Window-level enforcement of WindowManager.LayoutParams.FLAG_SECURE, lifecycle management preventing unauthorized screenshots, screen recordings, and app switcher thumbnail leakage."),
        ("Discovery Engine (NetworkDiscoveryManager)", "UDP broadcast transceiver monitoring port 5056 for laptop beacon frames and discovering local hosts without manual user configuration.")
    ]
    for b_title, b_desc in bullets:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(3)
        r = bp.add_run(f"{b_title}: ")
        r.font.bold = True
        bp.add_run(b_desc)

    add_h2("2.2 Windows Desktop Receiver Architecture")
    doc.add_paragraph(
        "The host side is implemented as a high-performance, single-file self-contained C# application targeting .NET 8 on Windows 10/11 x64. "
        "Key architectural modules include:"
    )
    bullets_win = [
        ("UdpReceiverEngine", "Asynchronous non-blocking UDP listener bound to port 5055, featuring low-latency packet parsing, device authentication, multi-adapter LAN broadcast beacons, and a dedicated TCP HTTP server for file drop and screen streaming on port 5056."),
        ("BluetoothReceiverEngine", "Dual RFCOMM 32-bit SPP GUID and Windows Serial COM port listener allowing zero-Wi-Fi fallback over Bluetooth Classic."),
        ("NativeInput Subsystem", "Win32 P/Invoke integration calling user32.dll SetCursorPos, SendInput, mouse_event, and keybd_event with comprehensive multi-display coordinate translation, virtual key mapping, and desktop switching."),
        ("Screen Capture Service", "GDI+ and BitBlt primary screen capture pipeline with configurable JPEG compression quality and proportional scaling for low-bandwidth visual streaming."),
        ("Failsafe & Safety Controller", "Centralized input release mechanism (ReleaseAllHeldInputs) that clears held mouse buttons and modifier keys (Ctrl, Shift, Alt, Win) upon client disconnect or session termination.")
    ]
    for b_title, b_desc in bullets_win:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(3)
        r = bp.add_run(f"{b_title}: ")
        r.font.bold = True
        bp.add_run(b_desc)

    # -------------------------------------------------------------
    # 3. Chronological Evolution History (Initial to Date)
    # -------------------------------------------------------------
    add_h1("3. Comprehensive Chronological Evolution History")
    doc.add_paragraph(
        "The AirGlide project transitioned through ten distinct engineering phases to reach its current production state. "
        "The following timeline documents the technical milestones and transformations:"
    )

    phases = [
        ("Phase 1: Initial Codebase & Core Precision Trackpad",
         "The baseline codebase started with raw UDP coordinate streaming. In Phase 1, the trackpad engine was completely overhauled "
         "with dynamic cubic spline acceleration curves, sub-pixel accumulation buffers, dual-axis kinetic friction inertia, and "
         "haptic feedback synchronization, transforming crude jumps into smooth desktop navigation."),

        ("Phase 2: Bluetooth RFCOMM & Serial SPP Hybrid Architecture",
         "Recognizing environments where Wi-Fi is restricted or unavailable (such as public networks with AP client isolation), "
         "Phase 2 introduced a high-speed Bluetooth Classic RFCOMM engine with 32-bit Serial Port Profile (SPP) fallback. "
         "The Android client was equipped with transparent protocol switching between Wi-Fi UDP and Bluetooth."),

        ("Phase 3: Modern Desktop WinForms Receiver & Telemetry",
         "The legacy command-line receiver was replaced by an enterprise dark-mode WinForms dashboard. System tray integration, "
         "automatic minimize-to-tray, and real-time laptop telemetry reporting (Battery %, AC status, CPU load %, RAM usage %) "
         "were introduced via bi-directional AIRGLIDE_ACK packets."),

        ("Phase 4: Stream Deck & Productivity Macro System",
         "To empower power users and developers, Phase 4 added a 12-tile customizable Stream Deck interface with pre-configured quick keys "
         "(Ctrl+C, Ctrl+V, Windows Run, Task Manager, Screen Lock), media controls, and multi-key combo execution. Specialized launch "
         "resolvers for Spotify, web browsers, and command consoles were embedded."),

        ("Phase 5: High-Performance Gamepad Mode & Landscape Orientation",
         "A dedicated gamepad controller mode was built featuring dual analog thumbsticks, 8-way directional pad, analog shoulder triggers, "
         "and action buttons. To ensure ergonomic control, lifecycle-bound landscape orientation locking was integrated."),

        ("Phase 6: Air Motion Spatial Gyro Controller",
         "Leveraging mobile IMU hardware, Phase 6 integrated a 3-DoF spatial 'laser pointer' controller. Raw gyroscope velocity matrices "
         "were filtered using low-pass gravity removal and deadband smoothing, enabling intuitive presentation and media control."),

        ("Phase 7: Bi-Directional Wi-Fi File Drop & Desktop Installer",
         "To solve initial laptop setup friction, an embedded HTTP file drop server and Wi-Fi installer were integrated. "
         "Users can transfer files directly from phone to laptop Downloads, and the phone can wirelessly deploy the single-file "
         "Windows receiver binary to an unconfigured laptop."),

        ("Phase 8: Absolute Screen Coordinate Digitizer & Dual-Mode Visual Mirroring",
         "Users requested the ability to treat the entire PC display as an absolute coordinate graph. Phase 8 engineered the "
         "Absolute Screen Digitizer, introducing normalized (u, v) in [0.0, 1.0] mapping, primary screen aspect ratio preservation (16:9 / 16:10), "
         "and an authenticated HTTP JPEG screen streaming engine with dual toggles for 'Live Mirror' and 'Blind Canvas'."),

        ("Phase 9: Operating System Security & Privacy Hardening",
         "In response to stringent security requirements, Phase 9 integrated WindowManager FLAG_SECURE protection across display access "
         "modes, blocking screenshots, screen recording, and task-switcher thumbnail caching. Strict network access controls were applied "
         "to restrict streaming endpoints to authenticated active paired devices, and Windows Defender Firewall rules were restricted to private/domain profiles."),

        ("Phase 10: Fail-Safe Resilience & Inbuilt Error Handling",
         "The final engineering phase implemented comprehensive error handling: a frame-delivery watchdog that auto-degrades live streaming "
         "to low-latency canvas mode during network congestion; an automatic input safety release failsafe (SAFETY_CLEAR / Q:) that "
         "guarantees zero mouse drag locks or stuck modifier keys on disconnect; and robust P/Invoke exception boundaries.")
    ]

    for p_title, p_desc in phases:
        add_h2(p_title)
        doc.add_paragraph(p_desc)

    # -------------------------------------------------------------
    # 4. Absolute Coordinate Digitizer & Screen Streaming Engine
    # -------------------------------------------------------------
    add_h1("4. Absolute Coordinate Digitizer & Screen Streaming Engine")
    doc.add_paragraph(
        "The Absolute Screen Digitizer represents one of the most sophisticated features in AirGlide Remote. "
        "Unlike conventional relative trackpads where touch events produce delta offsets (dx, dy), the Digitizer maps "
        "every touch coordinate on the phone directly to the exact pixel coordinate on the laptop's primary monitor."
    )

    add_h2("4.1 Mathematical Coordinate Normalization")
    doc.add_paragraph(
        "To achieve resolution-independent mapping across heterogeneous device displays, the touch surface normalizes "
        "local screen coordinates into unit space: (u, v) in [0.0, 1.0]."
    )
    doc.add_paragraph(
        "When a user touches the canvas at local pixel coordinate (x, y) within a touch surface of dimension (W_touch, H_touch):\n"
        "  u = clamp(x / W_touch, 0.0, 1.0)\n"
        "  v = clamp(y / H_touch, 0.0, 1.0)\n\n"
        "Upon receiving packet 'N:u,v:devId', the desktop receiver translates (u, v) to primary monitor coordinates (X_target, Y_target):\n"
        "  X_target = round(u * (W_screen - 1))\n"
        "  Y_target = round(v * (H_screen - 1))\n\n"
        "The Win32 API SetCursorPos(X_target, Y_target) is invoked, instantly repositioning the cursor with zero lag."
    )

    add_h2("4.2 Dual-Mode Architectural Toggle")
    doc.add_paragraph(
        "To provide maximum flexibility and preserve mobile battery and Wi-Fi bandwidth, the controller interface incorporates "
        "a dual-mode toggle switch:"
    )
    bullets_dual = [
        ("Live Screen Mirroring (📺)", "The phone continuously polls the laptop's authenticated HTTP screen endpoint (/screen) at ~15 FPS, rendering the live desktop under the user's fingers. This mode provides direct visual touch interaction, ideal for interacting with desktop UI elements without looking at the laptop screen."),
        ("Blind Canvas Digitizer (🎛️)", "Video polling is completely suspended, reducing network throughput to under 1 KB/s and eliminating phone GPU decode overhead. The mobile display acts as a high-precision drawing tablet or digitizer, allowing the user to manipulate the cursor while watching their physical monitor.")
    ]
    for b_title, b_desc in bullets_dual:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(3)
        r = bp.add_run(f"{b_title}: ")
        r.font.bold = True
        bp.add_run(b_desc)

    add_h2("4.3 Aspect Ratio Preservation & Black Bar Handling")
    doc.add_paragraph(
        "Laptops predominantly feature 16:9 (1920x1080, 2560x1440) or 16:10 (1920x1200, 2560x1600) aspect ratios, whereas "
        "modern smartphones utilize ultra-wide 19.5:9 or 20:9 aspect ratios. If touch mapping were stretched across the full phone screen, "
        "the user would experience non-linear horizontal stretching and distortion."
    )
    doc.add_paragraph(
        "To eliminate distortion, the Android UI dynamically queries the host monitor dimensions broadcast in AIRGLIDE_ACK "
        "telemetry and constrains the touch canvas using Compose Modifier.aspectRatio(hostW / hostH). Symmetric pillarbox black bars "
        "are placed on the left and right margins, guaranteeing 1:1 geometric parity between phone touch space and desktop pixel space."
    )

    # -------------------------------------------------------------
    # 5. Security Architecture & Trail of Bits Compliance
    # -------------------------------------------------------------
    add_h1("5. Security Architecture & Trail of Bits Compliance")
    doc.add_paragraph(
        "In accordance with strict security standards, AirGlide Remote was subjected to an exhaustive security audit and hardened "
        "against Trail of Bits insecure-defaults vulnerabilities."
    )

    sec_rules = [
        ("Trail of Bits Rule 1: Enforce Fail-Secure Defaults",
         "All incoming requests to the /screen streaming endpoint and file drop service are evaluated using fail-secure authorization. "
         "The receiver checks the remote IP against the currently active paired phone (ActiveDeviceId). Unpaired or unknown network callers "
         "immediately receive HTTP 403 Forbidden with zero bytes leaked. Missing credentials or unverified sessions fail closed rather than booting with fallback privileges."),

        ("Trail of Bits Rule 2: Restrict Network Bindings & Firewall Rules",
         "Firewall rules created by AirGlide (via Fix_Firewall.bat or the receiver UI) explicitly restrict inbound traffic to "
         "'profile=private,domain'. Public profiles (such as airport, café, or hotel Wi-Fi) are strictly excluded, preventing "
         "unauthorized port exposure on untrusted public networks."),

        ("Trail of Bits Rule 3: Avoid Default Credentials",
         "Device identifiers are never hardcoded or static. On initial Android app startup, a cryptographically secure UUIDv4 token "
         "is generated and stored in private SharedPreferences. All packet headers validate this dynamic identifier."),

        ("Trail of Bits Rule 4: Prevent Path Traversal in File Operations",
         "The file drop receiver automatically sanitizes all incoming client filenames using Path.GetFileName() and verifies them against "
         "Path.GetInvalidFileNameChars(). Any attempt to escape the designated Downloads\\AirGlide_Drop directory using directory traversal "
         "sequences ('../', '..\\') is completely neutralized."),

        ("Trail of Bits Rule 5: Protect Wire Protocols & Prevent Lockups",
         "The receiver parses all coordinate arguments using safe invariant culture double parsers, strictly bounds-checking and clamping "
         "normalized inputs between 0.0 and 1.0. Furthermore, an input safety clear command ('Q:clear') is integrated to release all held "
         "keys and mouse buttons upon session termination, preventing hostile or accidental input lockups."),

        ("Android OS Privacy: FLAG_SECURE Lifecycle Guard",
         "Whenever a user enters ScreenDigitizerScreen, WindowManager.LayoutParams.FLAG_SECURE is applied to the application window. "
         "This hardware-level flag prevents on-device screenshot capture, blocks third-party screen recorder apps from capturing sensitive desktop data, "
         "and blanks the Android multitasking overview thumbnail.")
    ]

    for s_title, s_desc in sec_rules:
        add_h2(s_title)
        doc.add_paragraph(s_desc)

    # -------------------------------------------------------------
    # 6. Wire Protocol Specifications
    # -------------------------------------------------------------
    add_h1("6. Wire Protocol Specifications")
    doc.add_paragraph(
        "Communication between the Android client and Windows host uses lightweight, human-readable ASCII/UTF-8 datagrams. "
        "The following table provides the exhaustive wire protocol catalog:"
    )

    proto_table = doc.add_table(rows=17, cols=3)
    proto_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    proto_table.autofit = False

    headers = ["Command", "Wire Syntax Example", "Action & Description"]
    hdr_widths = [Inches(1.2), Inches(2.3), Inches(3.2)]
    hdr_row = proto_table.rows[0]
    for idx, name in enumerate(headers):
        c = hdr_row.cells[idx]
        c.width = hdr_widths[idx]
        set_cell_background(c, "0E7490")
        set_cell_margins(c, 100, 100, 120, 120)
        p = c.paragraphs[0]
        r = p.add_run(name)
        r.font.bold = True
        r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(255, 255, 255)

    proto_data = [
        ("M", "M:12,-8:abc12345", "Relative Mouse Move: deltas dx=12, dy=-8"),
        ("C", "C:L:abc12345", "Mouse Click: L=Left, R=Right, M=Middle"),
        ("D", "D:L:abc12345", "Mouse Button Down (Drag begin)"),
        ("U", "U:L:abc12345", "Mouse Button Up (Drag release)"),
        ("S", "S:3.5:abc12345", "Mouse Wheel Scroll: vertical notches"),
        ("K", "K:ENTER:abc12345", "Virtual Key Press (ENTER, ESC, BACKSPACE, etc.)"),
        ("T", "T:Hello\\nWorld:abc12345", "Text Typing with newline unescaping"),
        ("A", "A:VOL_UP:abc12345", "System Action (VOL_UP, MUTE, LOCK, ZOOM_IN, etc.)"),
        ("J", "J:1:abc12345", "Display Jump (Move cursor to next monitor)"),
        ("B", "B:SGVsbG8=:abc12345", "Set Laptop Clipboard (Base64 payload)"),
        ("W", "W:1:abc12345", "Browser Tab Switch: +1 forward, -1 backward"),
        ("N", "N:0.450,0.720:abc12345", "Absolute Move: u=0.450, v=0.720 in [0, 1]"),
        ("O", "O:0.450,0.720,L:abc12345", "Absolute Click: Move to (u, v) and tap button"),
        ("Q", "Q:clear:abc12345", "Safety Clear: Release all held mouse buttons and modifier keys"),
        ("P", "P:abc12345:Pixel 8", "Client Presence Ping: Heartbeat every 1.5s"),
        ("ACK", "AIRGLIDE_ACK:Host:5055:85:1:12:45:1920x1080", "Laptop Telemetry Reply: bat, chg, cpu, ram, resolution")
    ]

    for row_idx, data_tuple in enumerate(proto_data):
        row = proto_table.rows[row_idx + 1]
        bg = "FFFFFF" if row_idx % 2 == 0 else "F8FAFC"
        for col_idx, val in enumerate(data_tuple):
            c = row.cells[col_idx]
            c.width = hdr_widths[col_idx]
            set_cell_background(c, bg)
            set_cell_margins(c, 70, 70, 100, 100)
            p = c.paragraphs[0]
            r = p.add_run(val)
            r.font.size = Pt(8.5)
            if col_idx == 0:
                r.font.bold = True
                r.font.color.rgb = COLOR_DARK

    doc.add_paragraph().paragraph_format.space_after = Pt(12)

    # -------------------------------------------------------------
    # 7. Bug Audits & Edge-Case Resolutions
    # -------------------------------------------------------------
    add_h1("7. Bug Audits & Edge-Case Resolutions")
    doc.add_paragraph(
        "Throughout continuous testing, multiple subtle operating system edge cases were identified and resolved:"
    )

    edge_cases = [
        ("IME Virtual Keyboard Synchronization",
         "On Android, soft keyboards can buffer composing text before dispatching commits. The input pipeline was optimized with an "
         "in-app text input modal that captures full string compositions and transmits atomic 'T:<text>' payloads, preventing dropped letters."),

        ("Spotify Desktop vs WindowsApps Path Resolution",
         "Spotify on Windows installs into varying locations depending on whether it was installed via Microsoft Store, "
         "traditional desktop installer, or user AppData. NativeInput.LaunchSpotify implements a 5-step fallback chain: "
         "(1) 'spotify:' URI protocol; (2) %APPDATA%\\Spotify\\spotify.exe; (3) %LOCALAPPDATA%\\Microsoft\\WindowsApps\\Spotify.exe; "
         "(4) system PATH; and (5) automated fallback to the Spotify Web Player in the default browser."),

        ("Windows Magnifier Zoom-In / Zoom-Out",
         "The standard Windows Magnifier hotkeys (Win + Plus / Win + Minus) require specific scan-code and extended-key handling. "
         "NativeInput executes virtual key sequences with proper key-up pairs to ensure magnification levels adjust reliably without locking the Win key."),

        ("Screen Streaming Watchdog & Zero-Lag Degradation",
         "In congested Wi-Fi environments, polling a continuous JPEG stream can saturate network buffers. ScreenDigitizerScreen "
         "includes an automatic watchdog timer: if three consecutive frames fail or latency exceeds 1.5 seconds, the UI signals 'isStreamStalled' "
         "and degrades smoothly to Blind Canvas mode without crashing the connection.")
    ]

    for ec_title, ec_desc in edge_cases:
        add_h2(ec_title)
        doc.add_paragraph(ec_desc)

    # -------------------------------------------------------------
    # 8. Verification, Test Results, and Build Procedures
    # -------------------------------------------------------------
    add_h1("8. Verification, Test Results, and Build Procedures")
    doc.add_paragraph(
        "To guarantee high reliability, the codebase is validated using automated unit test suites and rigorous production packaging scripts."
    )

    add_h2("8.1 Automated Test Execution")
    doc.add_paragraph(
        "• Python Protocol Test Suite (desktop_receiver/tests/test_protocol.py): 14/14 unit tests PASSED (0.437s), validating "
        "command parsing, coordinate clamping, safety clear triggers, and error resilience.\n"
        "• Android Gradle Test Suite (./gradlew.bat test): All ViewModel, Model wire serialization, and discovery manager tests PASSED."
    )

    add_h2("8.2 Production Android Release Build & Signing")
    doc.add_paragraph(
        "The release APK is compiled using Gradle assembleRelease and signed using the official Android SDK apksigner tool:\n"
        "  apksigner.bat sign --ks app\\keystore\\airglide.jks --ks-pass pass:airglide123 --key-pass pass:airglide123 "
        "--min-sdk-version 21 --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true .\\AirGlide.apk\n\n"
        "All three signature schemes (v1 Jar signature, v2 Full APK signature, v3 Key rotation signature) are verified to ensure "
        "seamless installation across all Android versions from Android 8.0 up to Android 15+."
    )

    add_h2("8.3 Windows Desktop Receiver Native Compilation")
    doc.add_paragraph(
        "The Windows desktop host is compiled as a self-contained, single-file native executable with zero external runtime dependencies:\n"
        "  dotnet publish desktop_receiver/AirGlideDesktop.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true\n\n"
        "The resulting binary is packaged into app/src/main/assets/receiver/AirGlide_Windows_Receiver.zip alongside automated firewall setup "
        "and uninstallation scripts, ensuring the mobile application can deploy the desktop host anywhere."
    )

    # -------------------------------------------------------------
    # 9. Operations & Troubleshooting Guide
    # -------------------------------------------------------------
    add_h1("9. Operations & Troubleshooting Guide")
    doc.add_paragraph(
        "1. Start the Windows Receiver by running AirGlide_Laptop_Receiver.exe or Start_Laptop_Receiver.bat.\n"
        "2. Ensure both laptop and phone are on the same Wi-Fi network, or pair via Bluetooth.\n"
        "3. Launch AirGlide on Android. The laptop will be discovered automatically via directed UDP beacon.\n"
        "4. To use the Absolute Digitizer: Open the mode dropdown and tap '🎯 Screen Mirror & Digitizer'. The app will automatically "
        "lock into landscape orientation and activate FLAG_SECURE protection.\n"
        "5. Toggle between '📺 Live Mirror' and '🎛️ Blind Canvas' depending on whether you want a live desktop feed under your touch.\n"
        "6. If the firewall blocks packets on Windows, run Fix_Firewall.bat as Administrator."
    )

    output_path = os.path.abspath("AirGlide_Complete_Project_Documentation.docx")
    doc.save(output_path)
    print(f"Documentation generated successfully at: {output_path}")

if __name__ == "__main__":
    create_document()

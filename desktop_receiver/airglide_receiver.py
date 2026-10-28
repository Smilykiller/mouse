#!/usr/bin/env python3
"""
AirGlide Desktop Receiver
=========================
Turns incoming UDP packets from the AirGlide phone app into real mouse / keyboard input.

  Windows : full support, no extra packages (uses the Win32 SendInput API)
  macOS / Linux : install "pynput" (pip install pynput) for mouse + keyboard support

Run:
    python airglide_receiver.py            # window (falls back to console if no GUI available)
    python airglide_receiver.py --nogui    # console only
    python airglide_receiver.py --dry-run  # no real input, just prints what would happen (testing)

Wire protocol (UTF-8 text, one command per UDP datagram):
    M:<dx>,<dy>:<dev>     relative mouse move          C:<L|R|M>:<dev>   click
    D:<L|R|M>:<dev>       button down                  U:<L|R|M>:<dev>   button up
    S:<notches>:<dev>     scroll wheel (+ = up)        K:<KEY>:<dev>     special key
    T:<text>:<dev>        type text (\\ \\: \\n escaped)  P:<dev>:<name>    presence / heartbeat
    AIRGLIDE_DISCOVERY_PING  -> we answer AIRGLIDE_SERVER_OFFER:<host>:<ip>:<port>
    P packets are answered with AIRGLIDE_ACK:<host>:<port> so the phone can show a real "Live" state.
We broadcast AIRGLIDE_BEACON:<host>:<ip>:<port> to UDP port 5056 (NEVER to our own port 5055).
"""

import argparse
import collections
import ctypes
import platform
import queue
import socket
import sys
import threading
import time

DEFAULT_PORT = 5055
DISCOVERY_PORT = 5056
VALID_COMMANDS = frozenset("MCDUSKTPX")
CONTROLLER_TIMEOUT = 10.0      # seconds of silence before another phone may take over
IS_WINDOWS = platform.system() == "Windows"


# --------------------------------------------------------------------------------------
# Protocol parsing (pure functions - unit tested in tests/test_protocol.py)
# --------------------------------------------------------------------------------------
Packet = collections.namedtuple("Packet", "cmd payload dev_id name")


def sanitize_name(name):
    return (name or "").replace(":", "-").strip() or "Laptop"


def unescape_text(s):
    """Reverse the phone's escaping:  \\\\ -> \\   \\: -> :   \\n -> newline."""
    out = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == "\\" and i + 1 < len(s):
            n = s[i + 1]
            out.append("\n" if n == "n" else n)
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def parse_packet(msg):
    """
    Returns a Packet, or None for anything that is not a valid phone command.
    Discovery traffic (AIRGLIDE_*) is handled separately and is NEVER input, which is the
    exact bug the old receiver had: it treated its own beacon as a controller called "5055".
    """
    msg = msg.strip()
    if not msg or msg.startswith("AIRGLIDE_"):
        return None
    cmd, sep, rest = msg.partition(":")
    if not sep or cmd not in VALID_COMMANDS:
        return None
    if cmd == "P":
        dev_id, _, name = rest.partition(":")
        if not dev_id:
            return None
        return Packet("P", "", dev_id, name or "Phone")
    payload, sep, dev_id = rest.rpartition(":")
    if not sep or not dev_id:
        return None
    return Packet(cmd, payload, dev_id, "")


class ControllerLock:
    """
    Only one phone may control the PC at a time (so a stranger's phone on the same Wi-Fi can't
    type on your laptop). The lock is released automatically after CONTROLLER_TIMEOUT seconds
    of silence, or manually with the "Release" button.
    """

    def __init__(self, timeout=CONTROLLER_TIMEOUT):
        self.timeout = timeout
        self.dev_id = None
        self.name = ""
        self.ip = ""
        self.last_seen = 0.0
        self.packets = 0

    def accept(self, dev_id, ip, now=None):
        now = time.time() if now is None else now
        if self.dev_id is None or dev_id == self.dev_id or now - self.last_seen > self.timeout:
            if dev_id != self.dev_id:
                self.dev_id = dev_id
                self.packets = 0
                self.name = ""
            self.ip = ip
            self.last_seen = now
            self.packets += 1
            return True
        return False

    def release(self):
        self.dev_id = None
        self.name = ""
        self.ip = ""
        self.packets = 0


# --------------------------------------------------------------------------------------
# Network helpers
# --------------------------------------------------------------------------------------
def _usable(ip):
    return bool(ip) and not ip.startswith("127.") and not ip.startswith("169.254.") and ip != "0.0.0.0"


def primary_ip(peer=None):
    """
    The address of the adapter that really reaches the network (the one with the default route),
    NOT the first adapter Windows lists - that is often a VirtualBox / VMware / WSL / VPN adapter,
    which was one reason the phone connected to an unreachable address.
    """
    targets = ([peer] if peer else []) + ["8.8.8.8", "192.168.0.1", "192.168.43.1", "10.0.0.1", "172.20.10.1"]
    for target in targets:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect((target, 9))      # UDP connect sends nothing; it only selects the route
            ip = s.getsockname()[0]
            s.close()
            if _usable(ip):
                return ip
        except OSError:
            pass
    return "127.0.0.1"


def all_local_ips():
    ips = []
    first = primary_ip()
    if _usable(first):
        ips.append(first)
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            ip = info[4][0]
            if _usable(ip) and ip not in ips:
                ips.append(ip)
    except OSError:
        pass
    return ips


# --------------------------------------------------------------------------------------
# Input back-ends
# --------------------------------------------------------------------------------------
class DryRunInput:
    """Records / prints actions instead of performing them (testing, unsupported OS)."""

    def __init__(self, echo=False):
        self.calls = []
        self.echo = echo

    def _rec(self, *call):
        self.calls.append(call)
        if self.echo:
            print("[dry-run]", *call)

    def move(self, dx, dy): self._rec("move", dx, dy)
    def button(self, btn, down): self._rec("down" if down else "up", btn)
    def click(self, btn): self._rec("click", btn)
    def scroll(self, notches): self._rec("scroll", notches)
    def key(self, name): self._rec("key", name)
    def text(self, text): self._rec("text", text)


# ---- Windows SendInput structures (defined everywhere so they can be unit-tested) ----
ULONG_PTR = ctypes.c_ulonglong if ctypes.sizeof(ctypes.c_void_p) == 8 else ctypes.c_ulong


# Fixed-width types: LONG = 32-bit, DWORD = 32-bit, WORD = 16-bit on Windows, and identical on every OS.
LONG, DWORD, WORD = ctypes.c_int32, ctypes.c_uint32, ctypes.c_uint16


class MOUSEINPUT(ctypes.Structure):
    _fields_ = [("dx", LONG), ("dy", LONG), ("mouseData", DWORD),
                ("dwFlags", DWORD), ("time", DWORD), ("dwExtraInfo", ULONG_PTR)]


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [("wVk", WORD), ("wScan", WORD), ("dwFlags", DWORD),
                ("time", DWORD), ("dwExtraInfo", ULONG_PTR)]


class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [("uMsg", DWORD), ("wParamL", WORD), ("wParamH", WORD)]


class _INPUT_UNION(ctypes.Union):
    _fields_ = [("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT)]


class INPUT(ctypes.Structure):
    _fields_ = [("type", DWORD), ("u", _INPUT_UNION)]


class WindowsInput:
    MOVE, LEFTDOWN, LEFTUP, RIGHTDOWN, RIGHTUP = 0x0001, 0x0002, 0x0004, 0x0008, 0x0010
    MIDDLEDOWN, MIDDLEUP, WHEEL = 0x0020, 0x0040, 0x0800
    KEYUP, UNICODE, EXTENDED = 0x0002, 0x0004, 0x0001
    BUTTONS = {"L": (0x0002, 0x0004), "R": (0x0008, 0x0010), "M": (0x0020, 0x0040)}
    VK = {"ENTER": 0x0D, "RETURN": 0x0D, "BACKSPACE": 0x08, "BKSP": 0x08, "TAB": 0x09, "SPACE": 0x20,
          "ESC": 0x1B, "ESCAPE": 0x1B, "SHIFT": 0x10, "LSHIFT": 0xA0, "RSHIFT": 0xA1,
          "CTRL": 0x11, "CONTROL": 0x11, "ALT": 0x12, "WIN": 0x5B, "WINDOWS": 0x5B, "SUPER": 0x5B,
          "UP": 0x26, "DOWN": 0x28, "LEFT": 0x25, "RIGHT": 0x27,
          "HOME": 0x24, "END": 0x23, "PAGE_UP": 0x21, "PAGEUP": 0x21, "PGUP": 0x21,
          "PAGE_DOWN": 0x22, "PAGEDOWN": 0x22, "PGDN": 0x22, "DELETE": 0x2E, "DEL": 0x2E,
          "INSERT": 0x2D, "INS": 0x2D, "CAPSLOCK": 0x14, "CAPS": 0x14,
          "PRINTSCREEN": 0x2C, "PRTSC": 0x2C,
          "F1": 0x70, "F2": 0x71, "F3": 0x72, "F4": 0x73, "F5": 0x74, "F6": 0x75,
          "F7": 0x76, "F8": 0x77, "F9": 0x78, "F10": 0x79, "F11": 0x7A, "F12": 0x7B}
    EXTENDED_KEYS = {0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x2D, 0x2E, 0x5B, 0x5C}

    def __init__(self):
        self.user32 = ctypes.windll.user32
        self.user32.SendInput.argtypes = (ctypes.c_uint, ctypes.POINTER(INPUT), ctypes.c_int)
        self.user32.SendInput.restype = ctypes.c_uint
        self._ensure_desktop()

    def _ensure_desktop(self):
        try:
            h_desk = self.user32.OpenInputDesktop(0, False, 0x01FF)
            if h_desk:
                self.user32.SetThreadDesktop(h_desk)
                self.user32.CloseDesktop(h_desk)
        except Exception:
            pass

    def _send(self, *items):
        arr = (INPUT * len(items))(*items)
        ret = self.user32.SendInput(len(items), arr, ctypes.sizeof(INPUT))
        if ret == 0:
            self._ensure_desktop()
            ret = self.user32.SendInput(len(items), arr, ctypes.sizeof(INPUT))
        return ret

    @staticmethod
    def _mouse(flags, dx=0, dy=0, data=0):
        i = INPUT()
        i.type = 0
        i.u.mi = MOUSEINPUT(dx, dy, data & 0xFFFFFFFF, flags, 0, 0)
        return i

    @staticmethod
    def _key(vk=0, scan=0, flags=0):
        i = INPUT()
        i.type = 1
        i.u.ki = KEYBDINPUT(vk, scan, flags, 0, 0)
        return i

    def move(self, dx, dy):
        if self._send(self._mouse(self.MOVE, int(dx), int(dy))) == 0:
            try:
                self.user32.mouse_event(self.MOVE, int(dx), int(dy), 0, 0)
            except Exception:
                pass

    def button(self, btn, down):
        d, u = self.BUTTONS.get(btn, self.BUTTONS["L"])
        self._send(self._mouse(d if down else u))

    def click(self, btn):
        d, u = self.BUTTONS.get(btn, self.BUTTONS["L"])
        self._send(self._mouse(d), self._mouse(u))

    def scroll(self, notches):
        self._send(self._mouse(self.WHEEL, data=int(notches) * 120))

    def key(self, name):
        name = name.upper().strip()
        vk = self.VK.get(name)
        if vk is not None:
            flags = self.EXTENDED if vk in self.EXTENDED_KEYS else 0
            if self._send(self._key(vk, 0, flags), self._key(vk, 0, flags | self.KEYUP)) == 0:
                try:
                    self.user32.keybd_event(vk, 0, flags, 0)
                    self.user32.keybd_event(vk, 0, flags | self.KEYUP, 0)
                except Exception:
                    pass
        elif len(name) == 1:
            self.text(name.lower())

    def text(self, text):
        for ch in text:
            if ch == "\n":
                self.key("ENTER")
            elif ch == "\t":
                self.key("TAB")
            else:
                data = ch.encode("utf-16-le")
                for i in range(0, len(data), 2):
                    unit = data[i] | (data[i + 1] << 8)
                    if self._send(self._key(0, unit, self.UNICODE),
                                  self._key(0, unit, self.UNICODE | self.KEYUP)) == 0:
                        try:
                            scan = self.user32.VkKeyScanW(ord(ch))
                            if scan != -1:
                                vk = scan & 0xFF
                                shift = bool(scan & 0x0100)
                                if shift: self.user32.keybd_event(0x10, 0, 0, 0)
                                self.user32.keybd_event(vk, 0, 0, 0)
                                self.user32.keybd_event(vk, 0, self.KEYUP, 0)
                                if shift: self.user32.keybd_event(0x10, 0, self.KEYUP, 0)
                        except Exception:
                            pass


class PynputInput:
    """macOS / Linux back-end (pip install pynput)."""

    def __init__(self):
        from pynput import keyboard, mouse  # imported lazily: optional dependency
        self.m = mouse.Controller()
        self.k = keyboard.Controller()
        self.Button = mouse.Button
        self.Key = keyboard.Key
        self.map = {"ENTER": self.Key.enter, "BACKSPACE": self.Key.backspace, "BKSP": self.Key.backspace,
                    "TAB": self.Key.tab, "SPACE": self.Key.space, "ESC": self.Key.esc,
                    "UP": self.Key.up, "DOWN": self.Key.down, "LEFT": self.Key.left,
                    "RIGHT": self.Key.right, "HOME": self.Key.home, "END": self.Key.end,
                    "PAGE_UP": self.Key.page_up, "PAGEUP": self.Key.page_up,
                    "PAGE_DOWN": self.Key.page_down, "PAGEDOWN": self.Key.page_down,
                    "DELETE": self.Key.delete}

    def _btn(self, b):
        return {"L": self.Button.left, "R": self.Button.right, "M": self.Button.middle}.get(b, self.Button.left)

    def move(self, dx, dy): self.m.move(int(dx), int(dy))
    def button(self, btn, down): (self.m.press if down else self.m.release)(self._btn(btn))
    def click(self, btn): self.m.click(self._btn(btn))
    def scroll(self, notches): self.m.scroll(0, int(notches))

    def key(self, name):
        k = self.map.get(name.upper().strip())
        if k is not None:
            self.k.press(k)
            self.k.release(k)

    def text(self, text): self.k.type(text)


def make_input_backend(dry_run=False):
    if dry_run:
        return DryRunInput(echo=True), "dry-run (no real input)"
    if IS_WINDOWS:
        return WindowsInput(), "Windows SendInput"
    try:
        return PynputInput(), "pynput"
    except Exception as exc:  # ImportError or no display server
        return DryRunInput(echo=True), "NO INPUT BACKEND (%s) - run: pip install pynput" % exc


# --------------------------------------------------------------------------------------
# Receiver engine
# --------------------------------------------------------------------------------------
class ReceiverEngine:
    def __init__(self, backend, port=DEFAULT_PORT, log=print):
        self.backend = backend
        self.port = port
        self.log = log
        self.host = sanitize_name(socket.gethostname())
        self.lock = ControllerLock()
        self.total_packets = 0
        self.blocked_packets = 0
        self.running = False
        self.sock = None
        self._threads = []
        self._last_block_log = 0.0

    # ---- lifecycle ----
    def start(self):
        if self.running:
            return
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.bind(("0.0.0.0", self.port))      # raises OSError if the port is already taken
        sock.settimeout(0.5)
        self.sock = sock
        self.running = True
        for target in (self._listen_loop, self._beacon_loop):
            t = threading.Thread(target=target, daemon=True)
            t.start()
            self._threads.append(t)
        self.log("Listening on UDP port %d  (laptop IP %s)" % (self.port, primary_ip()))

    def stop(self):
        if not self.running:
            return
        self.running = False
        try:
            self.sock.close()
        except OSError:
            pass
        self.sock = None
        self._threads = []
        self.log("Receiver stopped.")

    # ---- loops ----
    def _listen_loop(self):
        while self.running:
            sock = self.sock
            if sock is None:
                break
            try:
                data, addr = sock.recvfrom(2048)
            except socket.timeout:
                continue
            except OSError:
                if not self.running:
                    break
                continue
            try:
                self.handle_datagram(data.decode("utf-8", "ignore"), addr)
            except Exception as exc:  # never let one bad packet kill the loop
                self.log("Error handling packet: %s" % exc)

    def _beacon_loop(self):
        while self.running:
            for ip in all_local_ips():
                msg = ("AIRGLIDE_BEACON:%s:%s:%d" % (self.host, ip, self.port)).encode()
                destinations = {"255.255.255.255", ip.rsplit(".", 1)[0] + ".255"}
                try:
                    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
                    s.bind((ip, 0))     # send out of THIS adapter, so every network gets a beacon
                    for dest in destinations:
                        try:
                            s.sendto(msg, (dest, DISCOVERY_PORT))   # discovery port only!
                        except OSError:
                            pass
                    s.close()
                except OSError:
                    pass
            for _ in range(15):          # 1.5 s, but stop quickly
                if not self.running:
                    return
                time.sleep(0.1)

    # ---- packet handling ----
    def handle_datagram(self, text, addr, now=None):
        text = text.strip()
        if text.startswith("AIRGLIDE_DISCOVERY_PING"):
            offer = "AIRGLIDE_SERVER_OFFER:%s:%s:%d" % (self.host, primary_ip(addr[0]), self.port)
            self._reply(offer, addr)
            self.log("Discovery ping from %s answered" % addr[0])
            return

        pkt = parse_packet(text)
        if pkt is None:
            return
        self.total_packets += 1

        if not self.lock.accept(pkt.dev_id, addr[0], now):
            self.blocked_packets += 1
            t = time.time()
            if t - self._last_block_log > 5:
                self._last_block_log = t
                self.log("Ignored phone %s (%s): locked to %s. Press 'Release phone' to switch."
                         % (pkt.dev_id, addr[0], self.lock.dev_id))
            return

        if pkt.cmd == "P":
            if self.lock.name != pkt.name:
                self.lock.name = pkt.name
                self.log("Phone connected: %s (%s)" % (pkt.name, addr[0]))
            self._reply("AIRGLIDE_ACK:%s:%d" % (self.host, self.port), addr)
            return

        if pkt.cmd == "X":
            old_name = self.lock.name or pkt.dev_id
            self.lock.release()
            self._reply("AIRGLIDE_DISCONNECTED:%s:%d" % (self.host, self.port), addr)
            self.log("Phone disconnected: %s (%s)" % (old_name, addr[0]))
            return

        self.dispatch(pkt)

    def _reply(self, text, addr):
        try:
            if self.sock is not None:
                self.sock.sendto(text.encode(), addr)
        except OSError:
            pass

    def dispatch(self, pkt):
        b, cmd, p = self.backend, pkt.cmd, pkt.payload
        try:
            if cmd == "M":
                dx, dy = p.split(",")
                b.move(round(float(dx)), round(float(dy)))
            elif cmd == "C":
                b.click(p)
            elif cmd == "D":
                b.button(p, True)
            elif cmd == "U":
                b.button(p, False)
            elif cmd == "S":
                n = round(float(p))
                if n:
                    b.scroll(n)
            elif cmd == "K":
                b.key(p)
            elif cmd == "T":
                b.text(unescape_text(p))
        except ValueError:
            pass    # malformed numbers: ignore the packet


# --------------------------------------------------------------------------------------
# GUI
# --------------------------------------------------------------------------------------
def run_gui(engine, backend_name):
    import tkinter as tk
    from tkinter import messagebox

    log_queue = queue.Queue()
    engine.log = lambda msg: log_queue.put("[%s] %s" % (time.strftime("%H:%M:%S"), msg))

    bg, card, border = "#09090B", "#18181B", "#2E2E33"
    root = tk.Tk()
    root.title("AirGlide Desktop Receiver")
    root.geometry("640x560")
    root.minsize(540, 480)
    root.configure(bg=bg)

    header = tk.Frame(root, bg=bg)
    header.pack(fill="x", padx=18, pady=(16, 8))
    tk.Label(header, text="AirGlide Desktop", font=("Segoe UI", 16, "bold"), fg="#FAFAFA", bg=bg).pack(side="left")
    status = tk.Label(header, text="", font=("Segoe UI", 9, "bold"), bg=card, padx=10, pady=4)
    status.pack(side="right")

    ip_card = tk.Frame(root, bg=card, highlightbackground=border, highlightthickness=1, padx=16, pady=12)
    ip_card.pack(fill="x", padx=18, pady=6)
    tk.Label(ip_card, text="TYPE THIS IN THE PHONE APP IF IT IS NOT FOUND AUTOMATICALLY",
             font=("Segoe UI", 8, "bold"), fg="#71717A", bg=card).pack(anchor="w")
    row = tk.Frame(ip_card, bg=card)
    row.pack(fill="x", pady=(4, 2))
    ip_label = tk.Label(row, text=primary_ip(), font=("Consolas", 22, "bold"), fg="#FAFAFA", bg=card)
    ip_label.pack(side="left")
    tk.Label(row, text=":%d" % engine.port, font=("Consolas", 15), fg="#A1A1AA", bg=card).pack(side="left", padx=(4, 12))

    def copy_ip():
        root.clipboard_clear()
        root.clipboard_append(ip_label.cget("text"))

    tk.Button(row, text="Copy IP", relief="flat", bg=border, fg="#FAFAFA", padx=10, command=copy_ip).pack(side="left")
    others = tk.Label(ip_card, text="", font=("Segoe UI", 8), fg="#71717A", bg=card)
    others.pack(anchor="w")
    tk.Label(ip_card, text="Phone and laptop must be on the SAME Wi-Fi (or the phone's hotspot).",
             font=("Segoe UI", 8), fg="#22C55E", bg=card).pack(anchor="w", pady=(4, 0))

    dev_card = tk.Frame(root, bg=card, highlightbackground=border, highlightthickness=1, padx=16, pady=10)
    dev_card.pack(fill="x", padx=18, pady=6)
    tk.Label(dev_card, text="ACTIVE PHONE", font=("Segoe UI", 8, "bold"), fg="#71717A", bg=card).pack(anchor="w")
    dev_label = tk.Label(dev_card, text="Waiting for the phone...", font=("Segoe UI", 11, "bold"), fg="#A1A1AA", bg=card)
    dev_label.pack(anchor="w", pady=(2, 0))
    stats = tk.Label(dev_card, text="", font=("Segoe UI", 9), fg="#71717A", bg=card)
    stats.pack(anchor="w")
    tk.Label(dev_card, text="Input: " + backend_name, font=("Segoe UI", 8), fg="#71717A", bg=card).pack(anchor="w")

    log_frame = tk.Frame(root, bg=card, highlightbackground=border, highlightthickness=1)
    log_frame.pack(fill="both", expand=True, padx=18, pady=6)
    log_list = tk.Listbox(log_frame, bg=card, fg="#A1A1AA", font=("Consolas", 9), borderwidth=0, highlightthickness=0)
    log_list.pack(fill="both", expand=True, padx=8, pady=8)

    bottom = tk.Frame(root, bg=bg)
    bottom.pack(fill="x", padx=18, pady=(4, 14))
    toggle_btn = tk.Button(bottom, text="Stop", relief="flat", bg=border, fg="#FAFAFA", padx=14, pady=4)
    toggle_btn.pack(side="left")

    def toggle():
        if engine.running:
            engine.stop()
        else:
            try:
                engine.start()
            except OSError as exc:
                messagebox.showerror("AirGlide", "Cannot listen on UDP port %d:\n%s" % (engine.port, exc))

    toggle_btn.configure(command=toggle)
    tk.Button(bottom, text="Disconnect Phone", relief="flat", bg="#2D1414", fg="#F87171", padx=10, pady=4,
              font=("Segoe UI", 9, "bold"), command=engine.lock.release).pack(side="left", padx=8)
    tk.Button(bottom, text="Clear log", relief="flat", bg=card, fg="#A1A1AA", padx=10, pady=4,
              command=lambda: log_list.delete(0, tk.END)).pack(side="left")

    ticks = [0]

    def refresh():
        while True:
            try:
                log_list.insert(0, log_queue.get_nowait())
            except queue.Empty:
                break
        if log_list.size() > 80:
            log_list.delete(80, tk.END)
        if engine.running:
            status.configure(text="● LISTENING (%d)" % engine.port, fg="#22C55E")
            toggle_btn.configure(text="Stop")
        else:
            status.configure(text="● STOPPED", fg="#EF4444")
            toggle_btn.configure(text="Start")
        ticks[0] += 1
        if ticks[0] % 10 == 1:      # network lookups are slowish: refresh every ~5 s
            ip = primary_ip()
            ip_label.configure(text=ip)
            extra = [x for x in all_local_ips() if x != ip]
            others.configure(text=("Other adapters: " + ", ".join(extra)) if extra else "")
        lock = engine.lock
        if lock.dev_id and time.time() - lock.last_seen < CONTROLLER_TIMEOUT:
            dev_label.configure(text="Connected: %s" % (lock.name or lock.dev_id), fg="#22C55E")
            stats.configure(text="%d packets  •  IP %s  •  ignored from other phones: %d"
                            % (lock.packets, lock.ip, engine.blocked_packets))
        else:
            dev_label.configure(text="Waiting for the phone...", fg="#A1A1AA")
            stats.configure(text="%d packets received in total" % engine.total_packets)
        root.after(500, refresh)

    def on_close():
        engine.stop()
        root.destroy()

    root.protocol("WM_DELETE_WINDOW", on_close)
    refresh()
    root.mainloop()


def run_console(engine):
    print("AirGlide receiver is running. Press Ctrl+C to quit.")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        engine.stop()


def main():
    parser = argparse.ArgumentParser(description="AirGlide desktop receiver")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--nogui", action="store_true", help="console mode")
    parser.add_argument("--dry-run", action="store_true", help="do not move the real mouse; print actions")
    args = parser.parse_args()

    backend, backend_name = make_input_backend(args.dry_run)
    engine = ReceiverEngine(backend, port=args.port)
    try:
        engine.start()
    except OSError as exc:
        sys.exit("Cannot listen on UDP port %d: %s\n(Is another AirGlide receiver already running?)" % (args.port, exc))

    print("Input back-end:", backend_name)
    if args.nogui:
        run_console(engine)
        return
    try:
        import tkinter  # noqa: F401
        run_gui(engine, backend_name)
    except ImportError:
        print("tkinter not available - running in console mode.")
        run_console(engine)


if __name__ == "__main__":
    main()

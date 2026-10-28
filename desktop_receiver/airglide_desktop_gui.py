#!/usr/bin/env python3
"""
AirGlide Desktop Companion Server & Receiver
Cross-Platform GUI (Windows, macOS, Linux) with Zero Extra Dependencies
Includes Auto-Discovery Beacon (Mobile connects automatically!)
"""

import sys
import socket
import threading
import time
import platform
import tkinter as tk
from tkinter import ttk, messagebox

# OS-specific mouse and keyboard automation
IS_WINDOWS = platform.system() == "Windows"
IS_MAC = platform.system() == "Darwin"
IS_LINUX = platform.system() == "Linux"

if IS_WINDOWS:
    import ctypes
    user32 = ctypes.windll.user32
    MOUSEEVENTF_MOVE = 0x0001
    MOUSEEVENTF_LEFTDOWN = 0x0002
    MOUSEEVENTF_LEFTUP = 0x0004
    MOUSEEVENTF_RIGHTDOWN = 0x0008
    MOUSEEVENTF_RIGHTUP = 0x0010
    MOUSEEVENTF_MIDDLEDOWN = 0x0020
    MOUSEEVENTF_MIDDLEUP = 0x0040
    MOUSEEVENTF_WHEEL = 0x0800

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

class NativeInput:
    @staticmethod
    def move(dx, dy):
        if IS_WINDOWS:
            user32.mouse_event(MOUSEEVENTF_MOVE, int(round(dx)), int(round(dy)), 0, 0)

    @staticmethod
    def click(btn="L"):
        if IS_WINDOWS:
            if btn == "R":
                user32.mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
                user32.mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
            elif btn == "M":
                user32.mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, 0)
                user32.mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)
            else:
                user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
                user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)

    @staticmethod
    def mouse_down(btn="L"):
        if IS_WINDOWS:
            if btn == "R":
                user32.mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
            elif btn == "M":
                user32.mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, 0)
            else:
                user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)

    @staticmethod
    def mouse_up(btn="L"):
        if IS_WINDOWS:
            if btn == "R":
                user32.mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
            elif btn == "M":
                user32.mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)
            else:
                user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)

    @staticmethod
    def scroll(delta):
        if IS_WINDOWS:
            wheel_delta = int(round(delta * 120))
            user32.mouse_event(MOUSEEVENTF_WHEEL, 0, 0, wheel_delta, 0)

    @staticmethod
    def key_press(key):
        if IS_WINDOWS:
            # Common keys mapped via Windows SendKeys or vk
            vk_map = {
                "PAGE_UP": 0x21, "PAGEUP": 0x21,
                "PAGE_DOWN": 0x22, "PAGEDOWN": 0x22,
                "ENTER": 0x0D,
                "BACKSPACE": 0x08,
                "TAB": 0x09,
                "SPACE": 0x20,
                "ESC": 0x1B,
                "UP": 0x26, "DOWN": 0x28, "LEFT": 0x25, "RIGHT": 0x27
            }
            k = key.upper().strip()
            vk = vk_map.get(k)
            if vk:
                user32.keybd_event(vk, 0, 0, 0)
                user32.keybd_event(vk, 0, 2, 0) # 2 = KEYUP

    @staticmethod
    def type_text(text):
        if IS_WINDOWS:
            for char in text:
                vk = ord(char)
                # Send unicode char via KEYEVENTF_UNICODE (0x0004)
                user32.keybd_event(0, vk, 0x0004, 0)
                user32.keybd_event(0, vk, 0x0004 | 0x0002, 0)

class AirGlideDesktopApp:
    def __init__(self, root):
        self.root = root
        self.root.title("AirGlide Desktop Receiver")
        self.root.geometry("620x520")
        self.root.configure(bg="#09090B")
        self.root.minsize(520, 440)

        self.port = 5055
        self.local_ip = get_local_ip()
        self.hostname = socket.gethostname()
        self.is_running = False
        self.active_device_id = None
        self.packet_count = 0
        self.lock = threading.Lock()

        self.setup_ui()
        self.start_server()

    def setup_ui(self):
        # Header Frame
        header = tk.Frame(self.root, bg="#09090B")
        header.pack(fill="x", padx=18, pady=(16, 10))

        title = tk.Label(header, text="AirGlide Desktop", font=("Segoe UI", 16, "bold"), fg="#FAFAFA", bg="#09090B")
        title.pack(side="left")

        self.status_pill = tk.Label(header, text="● LISTENING (5055)", font=("Segoe UI", 9, "bold"), fg="#22C55E", bg="#18181B", padx=10, pady=4)
        self.status_pill.pack(side="right")

        # IP Card Frame
        card1 = tk.Frame(self.root, bg="#18181B", highlightbackground="#2E2E33", highlightthickness=1, padx=16, pady=12)
        card1.pack(fill="x", padx=18, pady=8)

        lbl_instruct = tk.Label(card1, text="CONNECT MOBILE APP TO THIS LAPTOP", font=("Segoe UI", 8, "bold"), fg="#71717A", bg="#18181B")
        lbl_instruct.pack(anchor="w")

        ip_row = tk.Frame(card1, bg="#18181B")
        ip_row.pack(fill="x", pady=(4, 6))

        lbl_ip = tk.Label(ip_row, text=self.local_ip, font=("Consolas", 22, "bold"), fg="#FAFAFA", bg="#18181B")
        lbl_ip.pack(side="left")

        lbl_port = tk.Label(ip_row, text=f":{self.port}", font=("Consolas", 15), fg="#A1A1AA", bg="#18181B")
        lbl_port.pack(side="left", padx=(4, 12))

        btn_copy = tk.Button(ip_row, text="Copy IP", font=("Segoe UI", 9, "bold"), fg="#FAFAFA", bg="#2E2E33", activebackground="#3F3F46", relief="flat", padx=10, command=self.copy_ip)
        btn_copy.pack(side="left")

        lbl_beacon = tk.Label(card1, text="✓ Auto-Discovery active: mobile app on the same Wi-Fi will detect this laptop automatically.", font=("Segoe UI", 8), fg="#22C55E", bg="#18181B")
        lbl_beacon.pack(anchor="w", pady=(4, 0))

        # Active Device Card
        card2 = tk.Frame(self.root, bg="#18181B", highlightbackground="#2E2E33", highlightthickness=1, padx=16, pady=10)
        card2.pack(fill="x", padx=18, pady=8)

        lbl_dev_hdr = tk.Label(card2, text="ACTIVE MOBILE CONTROLLER", font=("Segoe UI", 8, "bold"), fg="#71717A", bg="#18181B")
        lbl_dev_hdr.pack(anchor="w")

        self.lbl_device = tk.Label(card2, text="Waiting for mobile phone connection...", font=("Segoe UI", 11, "bold"), fg="#A1A1AA", bg="#18181B")
        self.lbl_device.pack(anchor="w", pady=(2, 2))

        self.lbl_stats = tk.Label(card2, text="0 packets received", font=("Segoe UI", 9), fg="#71717A", bg="#18181B")
        self.lbl_stats.pack(anchor="w")

        # Log Box
        log_frame = tk.Frame(self.root, bg="#18181B", highlightbackground="#2E2E33", highlightthickness=1)
        log_frame.pack(fill="both", expand=True, padx=18, pady=8)

        self.log_list = tk.Listbox(log_frame, bg="#18181B", fg="#A1A1AA", selectbackground="#2E2E33", selectforeground="#FAFAFA", font=("Consolas", 9), borderwidth=0, highlightthickness=0)
        self.log_list.pack(fill="both", expand=True, padx=8, pady=8)

        # Bottom Bar
        bottom = tk.Frame(self.root, bg="#09090B")
        bottom.pack(fill="x", padx=18, pady=(6, 14))

        self.btn_toggle = tk.Button(bottom, text="Stop Server", font=("Segoe UI", 9, "bold"), fg="#FAFAFA", bg="#2E2E33", relief="flat", padx=14, pady=4, command=self.toggle_server)
        self.btn_toggle.pack(side="left")

        btn_clear = tk.Button(bottom, text="Clear Log", font=("Segoe UI", 9), fg="#A1A1AA", bg="#18181B", relief="flat", padx=10, pady=4, command=lambda: self.log_list.delete(0, tk.END))
        btn_clear.pack(side="left", padx=8)

    def log(self, text):
        def _append():
            ts = time.strftime("%H:%M:%S")
            self.log_list.insert(0, f"[{ts}] {text}")
            if self.log_list.size() > 60:
                self.log_list.delete(60, tk.END)
        self.root.after(0, _append)

    def copy_ip(self):
        self.root.clipboard_clear()
        self.root.clipboard_append(self.local_ip)
        messagebox.showinfo("AirGlide", f"Copied IP ({self.local_ip}) to clipboard!")

    def start_server(self):
        if self.is_running:
            return
        self.is_running = True
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", self.port))

        self.recv_thread = threading.Thread(target=self._listen_loop, daemon=True)
        self.recv_thread.start()

        self.beacon_thread = threading.Thread(target=self._beacon_loop, daemon=True)
        self.beacon_thread.start()

        self.log(f"Server started on UDP port {self.port} (IP: {self.local_ip})")
        self.status_pill.configure(text=f"● LISTENING ({self.port})", fg="#22C55E")
        self.btn_toggle.configure(text="Stop Server")

    def stop_server(self):
        self.is_running = False
        try:
            self.sock.close()
        except Exception:
            pass
        self.log("Server stopped.")
        self.status_pill.configure(text="● STOPPED", fg="#EF4444")
        self.btn_toggle.configure(text="Start Server")

    def toggle_server(self):
        if self.is_running:
            self.stop_server()
        else:
            self.start_server()

    def _beacon_loop(self):
        beacon_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        beacon_sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        beacon_msg = f"AIRGLIDE_BEACON:{self.hostname}:{self.local_ip}:{self.port}".encode('utf-8')

        while self.is_running:
            try:
                beacon_sock.sendto(beacon_msg, ('255.255.255.255', self.port))
                beacon_sock.sendto(beacon_msg, ('255.255.255.255', 5056))
            except Exception:
                pass
            time.sleep(1.5)
        beacon_sock.close()

    def _listen_loop(self):
        while self.is_running:
            try:
                data, addr = self.sock.recvfrom(2048)
                msg = data.decode('utf-8', errors='ignore').strip()
                if not msg:
                    continue

                self.packet_count += 1

                # Discovery Ping
                if msg.startswith("AIRGLIDE_DISCOVERY_PING"):
                    offer = f"AIRGLIDE_SERVER_OFFER:{self.hostname}:{self.local_ip}:{self.port}".encode('utf-8')
                    self.sock.sendto(offer, addr)
                    self.log(f"Responded to discovery ping from {addr[0]}")
                    continue

                # Wire format: CMD:PAYLOAD:DEVICE_ID
                first_colon = msg.find(':')
                if first_colon <= 0:
                    continue
                cmd = msg[:first_colon]
                remainder = msg[first_colon + 1:]

                last_colon = remainder.rfind(':')
                if last_colon >= 0 and cmd != 'P':
                    payload = remainder[:last_colon]
                    dev_id = remainder[last_colon + 1:]
                else:
                    payload = remainder
                    dev_id = "unknown"

                if self.active_device_id is None and dev_id != "unknown":
                    self.active_device_id = dev_id
                    self.root.after(0, lambda d=dev_id, a=addr[0]: self.set_device_info(d, a))

                if self.active_device_id and dev_id != self.active_device_id and cmd != 'P':
                    continue

                if cmd == 'P':
                    # P:deviceId:deviceName
                    parts = payload.split(':')
                    name = parts[1] if len(parts) >= 2 else dev_id
                    self.root.after(0, lambda n=name, a=addr[0]: self.set_device_info(n, a))
                elif cmd == 'M':
                    coords = payload.split(',')
                    if len(coords) == 2:
                        NativeInput.move(float(coords[0]), float(coords[1]))
                elif cmd == 'C':
                    NativeInput.click(payload)
                    self.log(f"CLICK: {payload}")
                elif cmd == 'D':
                    NativeInput.mouse_down(payload)
                    self.log(f"DOWN: {payload}")
                elif cmd == 'U':
                    NativeInput.mouse_up(payload)
                    self.log(f"UP: {payload}")
                elif cmd == 'S':
                    NativeInput.scroll(float(payload))
                    self.log(f"SCROLL: {payload}")
                elif cmd == 'K':
                    NativeInput.key_press(payload)
                    self.log(f"KEY: {payload}")
                elif cmd == 'T':
                    text = payload.replace('\\:', ':')
                    NativeInput.type_text(text)
                    self.log(f"TEXT: {text}")

                if self.packet_count % 30 == 0:
                    self.root.after(0, self.update_packet_count)

            except Exception as e:
                if not self.is_running:
                    break

    def set_device_info(self, name, ip):
        self.lbl_device.configure(text=f"Connected: {name}", fg="#22C55E")
        self.lbl_stats.configure(text=f"{self.packet_count} packets received • IP: {ip}")

    def update_packet_count(self):
        txt = self.lbl_stats.cget("text")
        if "•" in txt:
            ip_part = txt.split("•")[1]
            self.lbl_stats.configure(text=f"{self.packet_count} packets received •{ip_part}")
        else:
            self.lbl_stats.configure(text=f"{self.packet_count} packets received")

if __name__ == "__main__":
    root = tk.Tk()
    app = AirGlideDesktopApp(root)
    root.mainloop()

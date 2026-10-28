"""Run:  python -m unittest discover -s tests -v     (from the desktop_receiver folder)"""
import ctypes
import os
import socket
import sys
import time
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import airglide_receiver as ar  # noqa: E402


def android_escape(text):
    """Exactly what RemoteCommand.Text.toWireString() does on the phone."""
    return text.replace("\\", "\\\\").replace(":", "\\:").replace("\n", "\\n")


class ParseTests(unittest.TestCase):
    def test_move(self):
        p = ar.parse_packet("M:15,-8:a1b2c3d4")
        self.assertEqual((p.cmd, p.payload, p.dev_id), ("M", "15,-8", "a1b2c3d4"))

    def test_scroll_negative_and_click(self):
        self.assertEqual(ar.parse_packet("S:-3:dev").payload, "-3")
        self.assertEqual(ar.parse_packet("C:R:dev").payload, "R")

    def test_presence_name_may_contain_spaces(self):
        p = ar.parse_packet("P:dev1:Google Pixel 9")
        self.assertEqual((p.cmd, p.dev_id, p.name), ("P", "dev1", "Google Pixel 9"))

    def test_discovery_traffic_is_never_input(self):
        # THE original bug: own beacon looked like a controller called "5055".
        self.assertIsNone(ar.parse_packet("AIRGLIDE_BEACON:MYLAPTOP:192.168.1.5:5055"))
        self.assertIsNone(ar.parse_packet("AIRGLIDE_SERVER_OFFER:MYLAPTOP:192.168.1.5:5055"))
        self.assertIsNone(ar.parse_packet("AIRGLIDE_ACK:MYLAPTOP:5055"))

    def test_junk_ignored(self):
        for junk in ["", "hello", "Z:1:dev", "M:1,2", "M:1,2:", ":::"]:
            self.assertIsNone(ar.parse_packet(junk), junk)

    def test_disconnect_packet(self):
        pkt = ar.parse_packet("X:bye:dev1")
        self.assertIsNotNone(pkt)
        self.assertEqual(pkt.cmd, "X")
        self.assertEqual(pkt.dev_id, "dev1")

    def test_text_roundtrip_matches_android_escaping(self):
        for original in ["hello", "https://google.com", "a\\b", "line1\nline2", "c:\\temp\\x:y", "emoji 😀 ok"]:
            wire = "T:%s:dev1" % android_escape(original)
            pkt = ar.parse_packet(wire)
            self.assertEqual(pkt.dev_id, "dev1")
            self.assertEqual(ar.unescape_text(pkt.payload), original)


class LockTests(unittest.TestCase):
    def test_second_phone_blocked_until_timeout_or_release(self):
        lock = ar.ControllerLock(timeout=10)
        self.assertTrue(lock.accept("A", "1.1.1.1", now=100))
        self.assertFalse(lock.accept("B", "1.1.1.2", now=105))
        self.assertTrue(lock.accept("A", "1.1.1.1", now=106))
        self.assertTrue(lock.accept("B", "1.1.1.2", now=120))   # A silent for 14 s
        lock.release()
        self.assertTrue(lock.accept("A", "1.1.1.1", now=121))


class EngineTests(unittest.TestCase):
    def feed(self, engine, text, ip="192.168.1.50", now=None):
        engine.handle_datagram(text, (ip, 40000), now)

    def make(self):
        backend = ar.DryRunInput()
        return ar.ReceiverEngine(backend, log=lambda m: None), backend

    def test_self_beacon_does_not_lock_out_the_real_phone(self):
        engine, backend = self.make()
        self.feed(engine, "AIRGLIDE_BEACON:MYLAPTOP:192.168.1.5:5055", ip="192.168.1.5")
        self.feed(engine, "M:5,6:a1b2c3d4")
        self.assertEqual(backend.calls, [("move", 5, 6)])

    def test_all_commands(self):
        engine, backend = self.make()
        for line in ["M:3,-4:d", "C:L:d", "C:R:d", "D:L:d", "U:L:d", "S:2:d", "S:0:d", "K:ENTER:d",
                     "T:hi\\:there:d"]:
            self.feed(engine, line)
        self.assertEqual(backend.calls, [
            ("move", 3, -4), ("click", "L"), ("click", "R"), ("down", "L"), ("up", "L"),
            ("scroll", 2), ("key", "ENTER"), ("text", "hi:there")])

    def test_other_phone_is_ignored(self):
        engine, backend = self.make()
        self.feed(engine, "M:1,1:phoneA", now=1000)
        self.feed(engine, "M:9,9:phoneB", now=1003)
        self.assertEqual(backend.calls, [("move", 1, 1)])
        self.assertEqual(engine.blocked_packets, 1)

    def test_garbage_numbers_do_not_crash(self):
        engine, backend = self.make()
        self.feed(engine, "M:abc,def:d")
        self.feed(engine, "S:xyz:d")
        self.assertEqual(backend.calls, [])


class NetworkEndToEnd(unittest.TestCase):
    def test_real_udp_roundtrip(self):
        port = 55055
        backend = ar.DryRunInput()
        engine = ar.ReceiverEngine(backend, port=port, log=lambda m: None)
        engine.start()
        try:
            phone = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            phone.settimeout(2)
            # heartbeat -> ACK
            phone.sendto(b"P:abcd1234:Test Phone", ("127.0.0.1", port))
            data, _ = phone.recvfrom(512)
            self.assertTrue(data.decode().startswith("AIRGLIDE_ACK"))
            # discovery ping -> offer
            phone.sendto(b"AIRGLIDE_DISCOVERY_PING", ("127.0.0.1", port))
            data, _ = phone.recvfrom(512)
            self.assertTrue(data.decode().startswith("AIRGLIDE_SERVER_OFFER:"))
            # input
            phone.sendto(b"M:7,8:abcd1234", ("127.0.0.1", port))
            phone.sendto(b"C:L:abcd1234", ("127.0.0.1", port))
            time.sleep(0.4)
            self.assertEqual(backend.calls, [("move", 7, 8), ("click", "L")])
            phone.close()
        finally:
            engine.stop()


class WindowsStructTests(unittest.TestCase):
    def test_input_struct_size(self):
        # Windows requires sizeof(INPUT) == 40 on 64-bit and 28 on 32-bit, or SendInput silently fails.
        expected = 40 if ctypes.sizeof(ctypes.c_void_p) == 8 else 28
        self.assertEqual(ctypes.sizeof(ar.INPUT), expected)
        self.assertEqual(ctypes.sizeof(ar.MOUSEINPUT), 32 if ctypes.sizeof(ctypes.c_void_p) == 8 else 24)
        self.assertEqual(ctypes.sizeof(ar.KEYBDINPUT), 24 if ctypes.sizeof(ctypes.c_void_p) == 8 else 16)


if __name__ == "__main__":
    unittest.main()

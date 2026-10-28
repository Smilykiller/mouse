using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;

namespace AirGlideDesktop
{
    /// <summary>Mouse / keyboard injection through the Win32 SendInput API (works from any thread).</summary>
    public static class NativeInput
    {
        [DllImport("user32.dll", SetLastError = true)]
        private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

        [DllImport("user32.dll")]
        private static extern short VkKeyScan(char ch);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern IntPtr OpenInputDesktop(uint dwFlags, bool fInherit, uint dwDesiredAccess);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool SetThreadDesktop(IntPtr hDesktop);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool CloseDesktop(IntPtr hDesktop);

        [ThreadStatic]
        private static bool t_desktopAttached;

        public static void EnsureInputDesktop()
        {
            if (t_desktopAttached) return;
            try
            {
                // Attach the calling thread to the user's active input desktop (WinSta0\Default)
                IntPtr hDesk = OpenInputDesktop(0, false, 0x01FF);
                if (hDesk != IntPtr.Zero)
                {
                    if (SetThreadDesktop(hDesk))
                    {
                        t_desktopAttached = true;
                    }
                    CloseDesktop(hDesk);
                }
            }
            catch { }
        }

        private const uint INPUT_MOUSE = 0;
        private const uint INPUT_KEYBOARD = 1;

        private const uint MOUSEEVENTF_MOVE = 0x0001;
        private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
        private const uint MOUSEEVENTF_LEFTUP = 0x0004;
        private const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
        private const uint MOUSEEVENTF_RIGHTUP = 0x0010;
        private const uint MOUSEEVENTF_MIDDLEDOWN = 0x0020;
        private const uint MOUSEEVENTF_MIDDLEUP = 0x0040;
        private const uint MOUSEEVENTF_WHEEL = 0x0800;

        private const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
        private const uint KEYEVENTF_KEYUP = 0x0002;
        private const uint KEYEVENTF_UNICODE = 0x0004;

        private const ushort VK_RETURN = 0x0D;
        private const ushort VK_TAB = 0x09;

        [StructLayout(LayoutKind.Sequential)]
        private struct INPUT
        {
            public uint type;
            public InputUnion u;
        }

        [StructLayout(LayoutKind.Explicit)]
        private struct InputUnion
        {
            [FieldOffset(0)] public MOUSEINPUT mi;
            [FieldOffset(0)] public KEYBDINPUT ki;
            [FieldOffset(0)] public HARDWAREINPUT hi;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct MOUSEINPUT
        {
            public int dx;
            public int dy;
            public uint mouseData;
            public uint dwFlags;
            public uint time;
            public UIntPtr dwExtraInfo;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct KEYBDINPUT
        {
            public ushort wVk;
            public ushort wScan;
            public uint dwFlags;
            public uint time;
            public UIntPtr dwExtraInfo;
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct HARDWAREINPUT
        {
            public uint uMsg;
            public ushort wParamL;
            public ushort wParamH;
        }

        private static readonly int InputSize = Marshal.SizeOf(typeof(INPUT));

        private static readonly Dictionary<string, ushort> VkMap =
            new Dictionary<string, ushort>(StringComparer.OrdinalIgnoreCase)
            {
                ["ENTER"] = 0x0D,
                ["RETURN"] = 0x0D,
                ["BACKSPACE"] = 0x08,
                ["BKSP"] = 0x08,
                ["TAB"] = 0x09,
                ["SPACE"] = 0x20,
                ["ESC"] = 0x1B,
                ["ESCAPE"] = 0x1B,
                ["SHIFT"] = 0x10,
                ["LSHIFT"] = 0xA0,
                ["RSHIFT"] = 0xA1,
                ["CTRL"] = 0x11,
                ["CONTROL"] = 0x11,
                ["ALT"] = 0x12,
                ["WIN"] = 0x5B,
                ["WINDOWS"] = 0x5B,
                ["SUPER"] = 0x5B,
                ["UP"] = 0x26,
                ["DOWN"] = 0x28,
                ["LEFT"] = 0x25,
                ["RIGHT"] = 0x27,
                ["HOME"] = 0x24,
                ["END"] = 0x23,
                ["PAGE_UP"] = 0x21,
                ["PAGEUP"] = 0x21,
                ["PGUP"] = 0x21,
                ["PAGE_DOWN"] = 0x22,
                ["PAGEDOWN"] = 0x22,
                ["PGDN"] = 0x22,
                ["DELETE"] = 0x2E,
                ["DEL"] = 0x2E,
                ["INSERT"] = 0x2D,
                ["INS"] = 0x2D,
                ["CAPSLOCK"] = 0x14,
                ["CAPS"] = 0x14,
                ["PRINTSCREEN"] = 0x2C,
                ["PRTSC"] = 0x2C,
                ["F1"] = 0x70,
                ["F2"] = 0x71,
                ["F3"] = 0x72,
                ["F4"] = 0x73,
                ["F5"] = 0x74,
                ["F6"] = 0x75,
                ["F7"] = 0x76,
                ["F8"] = 0x77,
                ["F9"] = 0x78,
                ["F10"] = 0x79,
                ["F11"] = 0x7A,
                ["F12"] = 0x7B
            };

        // Navigation keys must carry the "extended" flag, otherwise Windows may treat them as numpad keys.
        private static bool IsExtended(ushort vk)
        {
            return (vk >= 0x21 && vk <= 0x28) || vk == 0x2D || vk == 0x2E || vk == 0x5B || vk == 0x5C;
        }

        private static void SendMouse(uint flags, int dx, int dy, uint data)
        {
            EnsureInputDesktop();
            INPUT input = new INPUT();
            input.type = INPUT_MOUSE;
            input.u.mi = new MOUSEINPUT
            {
                dx = dx,
                dy = dy,
                mouseData = data,
                dwFlags = flags,
                time = 0,
                dwExtraInfo = UIntPtr.Zero
            };
            uint result = SendInput(1, new INPUT[] { input }, InputSize);
            if (result == 0)
            {
                // Fallback to Win32 mouse_event
                mouse_event(flags, dx, dy, data, UIntPtr.Zero);
            }
        }

        private static INPUT KeyInput(ushort vk, ushort scan, uint flags)
        {
            INPUT input = new INPUT();
            input.type = INPUT_KEYBOARD;
            input.u.ki = new KEYBDINPUT
            {
                wVk = vk,
                wScan = scan,
                dwFlags = flags,
                time = 0,
                dwExtraInfo = UIntPtr.Zero
            };
            return input;
        }

        private static void SendVirtualKey(ushort vk)
        {
            EnsureInputDesktop();
            uint ext = IsExtended(vk) ? KEYEVENTF_EXTENDEDKEY : 0;
            INPUT[] inputs = new INPUT[]
            {
                KeyInput(vk, 0, ext),
                KeyInput(vk, 0, ext | KEYEVENTF_KEYUP)
            };
            uint res = SendInput((uint)inputs.Length, inputs, InputSize);
            if (res == 0)
            {
                keybd_event((byte)vk, 0, ext, UIntPtr.Zero);
                keybd_event((byte)vk, 0, ext | KEYEVENTF_KEYUP, UIntPtr.Zero);
            }
        }

        public static void Move(int dx, int dy)
        {
            SendMouse(MOUSEEVENTF_MOVE, dx, dy, 0);
        }

        public static void LeftClick()
        {
            MouseDown("L");
            MouseUp("L");
        }

        public static void RightClick()
        {
            MouseDown("R");
            MouseUp("R");
        }

        public static void MiddleClick()
        {
            MouseDown("M");
            MouseUp("M");
        }

        public static void MouseDown(string btn)
        {
            uint flag = btn == "R" ? MOUSEEVENTF_RIGHTDOWN : btn == "M" ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_LEFTDOWN;
            SendMouse(flag, 0, 0, 0);
        }

        public static void MouseUp(string btn)
        {
            uint flag = btn == "R" ? MOUSEEVENTF_RIGHTUP : btn == "M" ? MOUSEEVENTF_MIDDLEUP : MOUSEEVENTF_LEFTUP;
            SendMouse(flag, 0, 0, 0);
        }

        /// <summary>Positive = wheel up, negative = wheel down (one notch = 120 units).</summary>
        public static void Scroll(int notches)
        {
            SendMouse(MOUSEEVENTF_WHEEL, 0, 0, unchecked((uint)(notches * 120)));
        }

        public static void KeyPress(string keyName)
        {
            string k = keyName.Trim();
            if (VkMap.TryGetValue(k, out ushort vk))
            {
                SendVirtualKey(vk);
            }
            else if (k.Length == 1)
            {
                TypeText(k.ToLowerInvariant());
            }
        }

        public static void TypeText(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            EnsureInputDesktop();

            List<INPUT> inputs = new List<INPUT>();
            foreach (char c in text)
            {
                if (c == '\n')
                {
                    inputs.Add(KeyInput(VK_RETURN, 0, 0));
                    inputs.Add(KeyInput(VK_RETURN, 0, KEYEVENTF_KEYUP));
                }
                else if (c == '\t')
                {
                    inputs.Add(KeyInput(VK_TAB, 0, 0));
                    inputs.Add(KeyInput(VK_TAB, 0, KEYEVENTF_KEYUP));
                }
                else
                {
                    inputs.Add(KeyInput(0, c, KEYEVENTF_UNICODE));
                    inputs.Add(KeyInput(0, c, KEYEVENTF_UNICODE | KEYEVENTF_KEYUP));
                }
            }

            if (inputs.Count > 0)
            {
                uint res = SendInput((uint)inputs.Count, inputs.ToArray(), InputSize);
                if (res == 0)
                {
                    foreach (char c in text)
                    {
                        if (c == '\n')
                        {
                            keybd_event((byte)VK_RETURN, 0, 0, UIntPtr.Zero);
                            keybd_event((byte)VK_RETURN, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                        }
                        else
                        {
                            short scan = VkKeyScan(c);
                            if (scan != -1)
                            {
                                byte vk = (byte)(scan & 0xFF);
                                bool shift = (scan & 0x0100) != 0;
                                if (shift) keybd_event(0x10, 0, 0, UIntPtr.Zero);
                                keybd_event(vk, 0, 0, UIntPtr.Zero);
                                keybd_event(vk, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                                if (shift) keybd_event(0x10, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                            }
                        }
                    }
                }
            }
        }
    }
}

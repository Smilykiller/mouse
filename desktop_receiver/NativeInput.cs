using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;

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

        [DllImport("user32.dll")]
        public static extern bool LockWorkStation();

        [DllImport("user32.dll")]
        private static extern bool SetCursorPos(int X, int Y);

        [DllImport("user32.dll")]
        private static extern bool GetCursorPos(out POINT lpPoint);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool GetSystemTimes(out long lpIdleTime, out long lpKernelTime, out long lpUserTime);

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
        private class MEMORYSTATUSEX
        {
            public uint dwLength = (uint)Marshal.SizeOf(typeof(MEMORYSTATUSEX));
            public uint dwMemoryLoad;
            public ulong ullTotalPhys;
            public ulong ullAvailPhys;
            public ulong ullTotalPageFile;
            public ulong ullAvailPageFile;
            public ulong ullTotalVirtual;
            public ulong ullAvailVirtual;
            public ulong ullAvailExtendedVirtual;
        }

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool GlobalMemoryStatusEx([In, Out] MEMORYSTATUSEX lpBuffer);

        private static long _prevIdleTime = 0;
        private static long _prevKernelTime = 0;
        private static long _prevUserTime = 0;
        private static int _lastCpuUsage = 0;
        private static DateTime _lastCpuSample = DateTime.MinValue;

        public static (int batPct, bool isCharging, int cpuPct, int ramPct) GetLaptopTelemetry()
        {
            // 1. Battery Telemetry
            int batPct = 100;
            bool isCharging = true;
            try
            {
                var pwr = SystemInformation.PowerStatus;
                batPct = (int)Math.Round(pwr.BatteryLifePercent * 100f);
                if (batPct < 0 || batPct > 100) batPct = 100;
                isCharging = pwr.PowerLineStatus == PowerLineStatus.Online;
            }
            catch { }

            // 2. RAM Telemetry
            int ramPct = 50;
            try
            {
                var mem = new MEMORYSTATUSEX();
                if (GlobalMemoryStatusEx(mem))
                {
                    ramPct = (int)mem.dwMemoryLoad;
                }
            }
            catch { }

            // 3. CPU Telemetry
            int cpuPct = _lastCpuUsage;
            try
            {
                if ((DateTime.UtcNow - _lastCpuSample).TotalMilliseconds > 800)
                {
                    if (GetSystemTimes(out long idle, out long kernel, out long user))
                    {
                        if (_prevKernelTime != 0 && _prevUserTime != 0)
                        {
                            long total = (kernel - _prevKernelTime) + (user - _prevUserTime);
                            long idleDiff = idle - _prevIdleTime;
                            if (total > 0)
                            {
                                cpuPct = (int)Math.Max(0, Math.Min(100, 100 - (idleDiff * 100 / total)));
                                _lastCpuUsage = cpuPct;
                            }
                        }
                        _prevIdleTime = idle;
                        _prevKernelTime = kernel;
                        _prevUserTime = user;
                        _lastCpuSample = DateTime.UtcNow;
                    }
                }
            }
            catch { }

            return (batPct, isCharging, cpuPct, ramPct);
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct POINT
        {
            public int X;
            public int Y;
        }

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

        public static void DisplayJump()
        {
            try
            {
                Screen[] screens = Screen.AllScreens;
                if (screens.Length <= 1) return;

                GetCursorPos(out POINT pt);
                Point current = new Point(pt.X, pt.Y);
                int currentIndex = 0;
                for (int i = 0; i < screens.Length; i++)
                {
                    if (screens[i].Bounds.Contains(current))
                    {
                        currentIndex = i;
                        break;
                    }
                }

                int nextIndex = (currentIndex + 1) % screens.Length;
                Rectangle nextBounds = screens[nextIndex].Bounds;
                int targetX = nextBounds.X + nextBounds.Width / 2;
                int targetY = nextBounds.Y + nextBounds.Height / 2;
                SetCursorPos(targetX, targetY);
            }
            catch { }
        }

        public static void SwitchBrowserTab(int direction)
        {
            try
            {
                if (direction > 0)
                {
                    // Next Tab: Ctrl + Tab
                    keybd_event(0x11, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x11, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                }
                else
                {
                    // Prev Tab: Ctrl + Shift + Tab
                    keybd_event(0x11, 0, 0, UIntPtr.Zero);
                    keybd_event(0x10, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x10, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x11, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                }
            }
            catch { }
        }

        public static void SetClipboard(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            Thread t = new Thread(() =>
            {
                try
                {
                    Clipboard.SetText(text);
                }
                catch { }
            });
            t.SetApartmentState(ApartmentState.STA);
            t.Start();
        }

        public static void ExecuteAction(string actionId)
        {
            string act = actionId.ToUpperInvariant().Trim();
            switch (act)
            {
                case "CHROME":
                    TryLaunch("chrome.exe", "https://google.com");
                    break;
                case "VSCODE":
                    TryLaunch("code", "");
                    break;
                case "SPOTIFY":
                    TryLaunch("spotify.exe", "");
                    break;
                case "TERMINAL":
                    TryLaunch("wt.exe", "");
                    break;
                case "POWERSHELL":
                    TryLaunch("powershell.exe", "");
                    break;
                case "EXPLORER":
                    TryLaunch("explorer.exe", "");
                    break;
                case "TASKMGR":
                    TryLaunch("taskmgr.exe", "");
                    break;
                case "LOCK":
                    LockWorkStation();
                    break;
                case "DESKTOP":
                    // Win + D
                    keybd_event(0x5B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x44, 0, 0, UIntPtr.Zero);
                    keybd_event(0x44, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x5B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "SCREENSHOT":
                    // Win + Shift + S
                    keybd_event(0x5B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x10, 0, 0, UIntPtr.Zero);
                    keybd_event(0x53, 0, 0, UIntPtr.Zero);
                    keybd_event(0x53, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x10, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x5B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "MEDIA_PLAY_PAUSE":
                    keybd_event(0xB3, 0, 0, UIntPtr.Zero);
                    keybd_event(0xB3, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "MEDIA_NEXT":
                    keybd_event(0xB0, 0, 0, UIntPtr.Zero);
                    keybd_event(0xB0, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "MEDIA_PREV":
                    keybd_event(0xB1, 0, 0, UIntPtr.Zero);
                    keybd_event(0xB1, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "VOLUME_MUTE":
                    keybd_event(0xAD, 0, 0, UIntPtr.Zero);
                    keybd_event(0xAD, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "VOLUME_UP":
                    keybd_event(0xAF, 0, 0, UIntPtr.Zero);
                    keybd_event(0xAF, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "VOLUME_DOWN":
                    keybd_event(0xAE, 0, 0, UIntPtr.Zero);
                    keybd_event(0xAE, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "CLOSE_APP":
                    // Alt + F4
                    keybd_event(0x12, 0, 0, UIntPtr.Zero);
                    keybd_event(0x73, 0, 0, UIntPtr.Zero);
                    keybd_event(0x73, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x12, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "FORMAT_CODE":
                    // Shift + Alt + F
                    keybd_event(0x10, 0, 0, UIntPtr.Zero);
                    keybd_event(0x12, 0, 0, UIntPtr.Zero);
                    keybd_event(0x46, 0, 0, UIntPtr.Zero);
                    keybd_event(0x46, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x12, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x10, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "MAGNIFIER":
                    // Win + Plus
                    keybd_event(0x5B, 0, 0, UIntPtr.Zero);
                    keybd_event(0xBB, 0, 0, UIntPtr.Zero);
                    keybd_event(0xBB, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x5B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;

                // --- Windows Multi-Finger Gestures ---
                case "WIN_TAB":
                    // Win + Tab (Task View)
                    keybd_event(0x5B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x5B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "WIN_D":
                    // Win + D (Show Desktop)
                    keybd_event(0x5B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x44, 0, 0, UIntPtr.Zero);
                    keybd_event(0x44, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x5B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "ALT_TAB":
                    // Alt + Tab (Switch App)
                    keybd_event(0x12, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x12, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "WIN_A":
                    // Win + A (Action Center / Notifications)
                    keybd_event(0x5B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x41, 0, 0, UIntPtr.Zero);
                    keybd_event(0x41, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    keybd_event(0x5B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "ZOOM_IN":
                    // Ctrl + Wheel Up
                    keybd_event(0x11, 0, 0, UIntPtr.Zero);
                    mouse_event(MOUSEEVENTF_WHEEL, 0, 0, 120, UIntPtr.Zero);
                    keybd_event(0x11, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "ZOOM_OUT":
                    // Ctrl + Wheel Down
                    keybd_event(0x11, 0, 0, UIntPtr.Zero);
                    mouse_event(MOUSEEVENTF_WHEEL, 0, 0, unchecked((uint)-120), UIntPtr.Zero);
                    keybd_event(0x11, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;

                // --- Presentation Remote Mode ---
                case "SLIDE_NEXT":
                    // Page Down or Right Arrow
                    keybd_event(0x22, 0, 0, UIntPtr.Zero);
                    keybd_event(0x22, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "SLIDE_PREV":
                    // Page Up or Left Arrow
                    keybd_event(0x21, 0, 0, UIntPtr.Zero);
                    keybd_event(0x21, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "SLIDE_START":
                    // F5 (Start Slideshow)
                    keybd_event(0x74, 0, 0, UIntPtr.Zero);
                    keybd_event(0x74, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "SLIDE_END":
                    // Escape (Exit Slideshow)
                    keybd_event(0x1B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x1B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "SLIDE_BLANK_B":
                    // 'B' key (Blank black screen in PPT/Keynote)
                    keybd_event(0x42, 0, 0, UIntPtr.Zero);
                    keybd_event(0x42, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "SLIDE_BLANK_W":
                    // 'W' key (Blank white screen in PPT/Keynote)
                    keybd_event(0x57, 0, 0, UIntPtr.Zero);
                    keybd_event(0x57, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;

                // --- Media Navigation ---
                case "MEDIA_REWIND":
                    // Left Arrow (or 'J' in YouTube)
                    keybd_event(0x25, 0, 0, UIntPtr.Zero);
                    keybd_event(0x25, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "MEDIA_FORWARD":
                    // Right Arrow (or 'L' in YouTube)
                    keybd_event(0x27, 0, 0, UIntPtr.Zero);
                    keybd_event(0x27, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;

                // --- Virtual Gamepad Mode ---
                case "GP_UP":
                    keybd_event(0x26, 0, 0, UIntPtr.Zero);
                    keybd_event(0x26, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_DOWN":
                    keybd_event(0x28, 0, 0, UIntPtr.Zero);
                    keybd_event(0x28, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_LEFT":
                    keybd_event(0x25, 0, 0, UIntPtr.Zero);
                    keybd_event(0x25, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_RIGHT":
                    keybd_event(0x27, 0, 0, UIntPtr.Zero);
                    keybd_event(0x27, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_A":
                    // Space
                    keybd_event(0x20, 0, 0, UIntPtr.Zero);
                    keybd_event(0x20, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_B":
                    // Escape
                    keybd_event(0x1B, 0, 0, UIntPtr.Zero);
                    keybd_event(0x1B, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_X":
                    // 'Z' key
                    keybd_event(0x5A, 0, 0, UIntPtr.Zero);
                    keybd_event(0x5A, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_Y":
                    // 'X' key
                    keybd_event(0x58, 0, 0, UIntPtr.Zero);
                    keybd_event(0x58, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_LB":
                    // 'Q' key
                    keybd_event(0x51, 0, 0, UIntPtr.Zero);
                    keybd_event(0x51, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_RB":
                    // 'E' key
                    keybd_event(0x45, 0, 0, UIntPtr.Zero);
                    keybd_event(0x45, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_START":
                    // Enter
                    keybd_event(0x0D, 0, 0, UIntPtr.Zero);
                    keybd_event(0x0D, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;
                case "GP_SELECT":
                    // Tab
                    keybd_event(0x09, 0, 0, UIntPtr.Zero);
                    keybd_event(0x09, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                    break;

                default:
                    if (act.StartsWith("HOTKEY:", StringComparison.OrdinalIgnoreCase))
                    {
                        ExecuteCustomHotkey(act.Substring(7));
                    }
                    break;
            }
        }

        private static void ExecuteCustomHotkey(string hotkeySpec)
        {
            try
            {
                string[] parts = hotkeySpec.Split(new[] { '+', '-' }, StringSplitOptions.RemoveEmptyEntries);
                List<byte> pressedModifiers = new List<byte>();
                byte mainKey = 0;

                foreach (string rawPart in parts)
                {
                    string p = rawPart.Trim().ToUpperInvariant();
                    switch (p)
                    {
                        case "CTRL":
                        case "CONTROL":
                            pressedModifiers.Add(0x11);
                            break;
                        case "SHIFT":
                            pressedModifiers.Add(0x10);
                            break;
                        case "ALT":
                            pressedModifiers.Add(0x12);
                            break;
                        case "WIN":
                        case "WINDOWS":
                            pressedModifiers.Add(0x5B);
                            break;
                        case "ESC":
                        case "ESCAPE":
                            mainKey = 0x1B;
                            break;
                        case "ENTER":
                            mainKey = 0x0D;
                            break;
                        case "TAB":
                            mainKey = 0x09;
                            break;
                        case "SPACE":
                            mainKey = 0x20;
                            break;
                        case "DELETE":
                        case "DEL":
                            mainKey = 0x2E;
                            break;
                        default:
                            if (p.Length == 1)
                            {
                                short vk = VkKeyScan(p[0]);
                                if (vk != -1) mainKey = (byte)(vk & 0xFF);
                            }
                            else if (p.StartsWith("F") && int.TryParse(p.Substring(1), out int fNum) && fNum >= 1 && fNum <= 12)
                            {
                                mainKey = (byte)(0x6F + fNum);
                            }
                            break;
                    }
                }

                // Press modifiers down
                foreach (byte mod in pressedModifiers)
                {
                    keybd_event(mod, 0, 0, UIntPtr.Zero);
                }

                // Tap main key
                if (mainKey != 0)
                {
                    keybd_event(mainKey, 0, 0, UIntPtr.Zero);
                    keybd_event(mainKey, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                }

                // Release modifiers in reverse
                for (int i = pressedModifiers.Count - 1; i >= 0; i--)
                {
                    keybd_event(pressedModifiers[i], 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                }
            }
            catch { }
        }

        private static void TryLaunch(string fileName, string args)
        {
            try
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = fileName,
                    Arguments = args,
                    UseShellExecute = true
                });
            }
            catch { }
        }
    }
}

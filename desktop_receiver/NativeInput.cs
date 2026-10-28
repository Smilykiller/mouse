using System;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    public static class NativeInput
    {
        [DllImport("user32.dll", SetLastError = true)]
        private static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

        [DllImport("user32.dll", SetLastError = true)]
        private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

        private const uint MOUSEEVENTF_MOVE = 0x0001;
        private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
        private const uint MOUSEEVENTF_LEFTUP = 0x0004;
        private const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
        private const uint MOUSEEVENTF_RIGHTUP = 0x0010;
        private const uint MOUSEEVENTF_MIDDLEDOWN = 0x0020;
        private const uint MOUSEEVENTF_MIDDLEUP = 0x0040;
        private const uint MOUSEEVENTF_WHEEL = 0x0800;

        private const int INPUT_KEYBOARD = 1;
        private const uint KEYEVENTF_KEYUP = 0x0002;
        private const uint KEYEVENTF_UNICODE = 0x0004;

        [StructLayout(LayoutKind.Sequential)]
        private struct INPUT
        {
            public int type;
            public InputUnion u;
        }

        [StructLayout(LayoutKind.Explicit)]
        private struct InputUnion
        {
            [FieldOffset(0)]
            public MOUSEINPUT mi;
            [FieldOffset(0)]
            public KEYBDINPUT ki;
            [FieldOffset(0)]
            public HARDWAREINPUT hi;
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

        public static void Move(int dx, int dy)
        {
            try
            {
                INPUT[] inputs = new INPUT[1];
                inputs[0].type = 0; // INPUT_MOUSE
                inputs[0].u.mi = new MOUSEINPUT
                {
                    dx = dx,
                    dy = dy,
                    mouseData = 0,
                    dwFlags = MOUSEEVENTF_MOVE,
                    time = 0,
                    dwExtraInfo = UIntPtr.Zero
                };
                if (SendInput(1, inputs, Marshal.SizeOf(typeof(INPUT))) == 0)
                {
                    mouse_event(MOUSEEVENTF_MOVE, dx, dy, 0, UIntPtr.Zero);
                }
            }
            catch
            {
                mouse_event(MOUSEEVENTF_MOVE, dx, dy, 0, UIntPtr.Zero);
            }
        }

        public static void LeftClick()
        {
            mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
            mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
        }

        public static void RightClick()
        {
            mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, UIntPtr.Zero);
            mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, UIntPtr.Zero);
        }

        public static void MiddleClick()
        {
            mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, UIntPtr.Zero);
            mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, UIntPtr.Zero);
        }

        public static void MouseDown(string btn)
        {
            if (btn == "R") mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, UIntPtr.Zero);
            else if (btn == "M") mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, UIntPtr.Zero);
            else mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
        }

        public static void MouseUp(string btn)
        {
            if (btn == "R") mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, UIntPtr.Zero);
            else if (btn == "M") mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, UIntPtr.Zero);
            else mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
        }

        public static void Scroll(int delta)
        {
            uint wheelDelta = unchecked((uint)(delta * 120));
            mouse_event(MOUSEEVENTF_WHEEL, 0, 0, wheelDelta, UIntPtr.Zero);
        }

        public static void KeyPress(string keyName)
        {
            string k = keyName.ToUpperInvariant().Trim();
            string? sendKeyStr = k switch
            {
                "PAGE_UP" or "PAGEUP" or "PGUP" => "{PGUP}",
                "PAGE_DOWN" or "PAGEDOWN" or "PGDN" => "{PGDN}",
                "ENTER" => "{ENTER}",
                "BACKSPACE" or "BKSP" => "{BACKSPACE}",
                "TAB" => "{TAB}",
                "SPACE" => " ",
                "ESC" or "ESCAPE" => "{ESC}",
                "UP" => "{UP}",
                "DOWN" => "{DOWN}",
                "LEFT" => "{LEFT}",
                "RIGHT" => "{RIGHT}",
                "HOME" => "{HOME}",
                "END" => "{END}",
                _ => null
            };

            if (sendKeyStr != null)
            {
                try
                {
                    SendKeys.SendWait(sendKeyStr);
                }
                catch { }
            }
            else if (k.Length == 1)
            {
                TypeText(k);
            }
        }

        public static void TypeText(string text)
        {
            if (string.IsNullOrEmpty(text)) return;

            INPUT[] inputs = new INPUT[text.Length * 2];
            int idx = 0;

            foreach (char c in text)
            {
                // Key down
                inputs[idx].type = INPUT_KEYBOARD;
                inputs[idx].u.ki.wVk = 0;
                inputs[idx].u.ki.wScan = c;
                inputs[idx].u.ki.dwFlags = KEYEVENTF_UNICODE;
                idx++;

                // Key up
                inputs[idx].type = INPUT_KEYBOARD;
                inputs[idx].u.ki.wVk = 0;
                inputs[idx].u.ki.wScan = c;
                inputs[idx].u.ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP;
                idx++;
            }

            SendInput((uint)inputs.Length, inputs, Marshal.SizeOf(typeof(INPUT)));
        }
    }
}

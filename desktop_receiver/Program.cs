using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    internal static class Program
    {
        [DllImport("shell32.dll", SetLastError = true)]
        private static extern int SetCurrentProcessExplicitAppUserModelID([MarshalAs(UnmanagedType.LPWStr)] string AppID);

        [STAThread]
        static void Main(string[] args)
        {
            try
            {
                // Register explicit AppUserModelID so Windows 10/11 taskbar pinning and grouping works
                SetCurrentProcessExplicitAppUserModelID(WindowsShortcutHelper.AppUserModelId);
            }
            catch { }

            // Ensure Start Menu shortcut exists so search displays the custom logo and enables Pin to Taskbar
            try
            {
                if (!File.Exists(WindowsShortcutHelper.StartMenuShortcutPath))
                {
                    WindowsShortcutHelper.InstallAndCreateShortcuts();
                }
            }
            catch { }

            ApplicationConfiguration.Initialize();
            bool startMinimized = args.Length > 0 && args[0].Equals("--minimized", StringComparison.OrdinalIgnoreCase);
            Application.Run(new MainForm(startMinimized));
        }
    }
}

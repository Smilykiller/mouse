using System;
using System.Diagnostics;
using System.IO;
using Microsoft.Win32;

namespace AirGlideDesktop
{
    public static class WindowsShortcutHelper
    {
        public const string AppUserModelId = "AirGlide.Remote.Desktop";
        public const string AppName = "AirGlide Remote";

        public static string InstallFolder => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "AirGlide");
        public static string InstalledExePath => Path.Combine(InstallFolder, "AirGlide.exe");
        public static string InstalledIconPath => Path.Combine(InstallFolder, "AirGlide.ico");
        public static string StartMenuShortcutPath => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), $"{AppName}.lnk");
        public static string DesktopShortcutPath => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), $"{AppName}.lnk");

        public static bool IsInstalled()
        {
            return File.Exists(InstalledExePath);
        }

        public static (bool Success, string Message) InstallAndCreateShortcuts()
        {
            try
            {
                Directory.CreateDirectory(InstallFolder);

                string currentExe = Process.GetCurrentProcess().MainModule?.FileName 
                    ?? AppDomain.CurrentDomain.BaseDirectory.TrimEnd('\\') + "\\AirGlide_Laptop_Receiver.exe";

                // 1. Copy executable to LocalAppData if not already there
                if (!string.Equals(currentExe, InstalledExePath, StringComparison.OrdinalIgnoreCase) && File.Exists(currentExe))
                {
                    try
                    {
                        File.Copy(currentExe, InstalledExePath, overwrite: true);
                    }
                    catch { }
                }

                // 2. Extract or copy the icon file
                string localIco = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Resources", "app_icon.ico");
                if (File.Exists(localIco))
                {
                    try { File.Copy(localIco, InstalledIconPath, overwrite: true); } catch { }
                }

                string targetExe = File.Exists(InstalledExePath) ? InstalledExePath : currentExe;
                string iconPath = File.Exists(InstalledIconPath) ? InstalledIconPath : targetExe;

                // 3. Create Start Menu Shortcut
                CreateShortcut(StartMenuShortcutPath, targetExe, iconPath, "AirGlide Remote - Wireless Trackpad & Keyboard");

                // 4. Create Desktop Shortcut
                CreateShortcut(DesktopShortcutPath, targetExe, iconPath, "AirGlide Remote");

                // 5. Register App Path in Windows Registry so 'airglide' works in Win+R Run dialog
                try
                {
                    using var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe");
                    key?.SetValue("", targetExe);
                    key?.SetValue("Path", Path.GetDirectoryName(targetExe) ?? "");
                }
                catch { }

                return (true, "AirGlide Remote has been added to your Start Menu and Desktop with its custom logo icon! You can now right-click it to Pin to Taskbar.");
            }
            catch (Exception ex)
            {
                return (false, $"Could not create shortcuts: {ex.Message}");
            }
        }

        private static void CreateShortcut(string shortcutPath, string targetExe, string iconPath, string description)
        {
            string psScript = $@"
$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut('{shortcutPath.Replace("'", "''")}')
$Shortcut.TargetPath = '{targetExe.Replace("'", "''")}'
$Shortcut.WorkingDirectory = '{Path.GetDirectoryName(targetExe)?.Replace("'", "''")}'
$Shortcut.IconLocation = '{iconPath.Replace("'", "''")}, 0'
$Shortcut.Description = '{description.Replace("'", "''")}'
$Shortcut.Save()
";
            RunPowerShell(psScript);
        }

        public static bool IsRunAtStartupEnabled()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", false);
                return key?.GetValue(AppName) != null;
            }
            catch { return false; }
        }

        public static void SetRunAtStartup(bool enable)
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", true);
                if (key == null) return;
                if (enable)
                {
                    string target = File.Exists(InstalledExePath) ? InstalledExePath : Process.GetCurrentProcess().MainModule?.FileName ?? "";
                    key.SetValue(AppName, $"\"{target}\" --minimized");
                }
                else
                {
                    key.DeleteValue(AppName, false);
                }
            }
            catch { }
        }

        private static void RunPowerShell(string script)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = $"-NoProfile -ExecutionPolicy Bypass -Command \"{script}\"",
                    CreateNoWindow = true,
                    UseShellExecute = false,
                    WindowStyle = ProcessWindowStyle.Hidden
                };
                using var p = Process.Start(psi);
                p?.WaitForExit(3000);
            }
            catch { }
        }
    }
}

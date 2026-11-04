using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;
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
        public static string InstalledUninstallBatPath => Path.Combine(InstallFolder, "Uninstall.bat");
        public static string StartMenuShortcutPath => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), $"{AppName}.lnk");
        public static string StartMenuUninstallShortcutPath => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), $"Uninstall {AppName}.lnk");
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

                // 5. Create Standalone Uninstall Script in InstallFolder
                try
                {
                    string uninstBatContent = $@"@echo off
title AirGlide Remote - Uninstaller
echo Terminating running AirGlide processes...
taskkill /F /IM AirGlide.exe /IM AirGlide_Laptop_Receiver.exe >nul 2>&1
echo Removing registry entries...
reg delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\Run"" /v ""AirGlide Remote"" /f >nul 2>&1
reg delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\Run"" /v ""AirGlide"" /f >nul 2>&1
reg delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe"" /f >nul 2>&1
reg delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide"" /f >nul 2>&1
netsh advfirewall firewall delete rule name=""AirGlide UDP"" >nul 2>&1
del /f /q ""%USERPROFILE%\Desktop\AirGlide Remote.lnk"" >nul 2>&1
del /f /q ""%APPDATA%\Microsoft\Windows\Start Menu\Programs\AirGlide Remote.lnk"" >nul 2>&1
del /f /q ""%APPDATA%\Microsoft\Windows\Start Menu\Programs\Uninstall AirGlide Remote.lnk"" >nul 2>&1
set ""TARGET_DIR=%LOCALAPPDATA%\AirGlide""
timeout /t 1 /nobreak >nul
start """" cmd /c ""timeout /t 1 /nobreak >nul & rd /s /q \""%TARGET_DIR%\"" >nul 2>&1 & exit""
echo AirGlide has been successfully uninstalled.
";
                    File.WriteAllText(InstalledUninstallBatPath, uninstBatContent);
                }
                catch { }

                // 6. Register App Path in Windows Registry so 'airglide' works in Win+R Run dialog
                try
                {
                    using var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe");
                    key?.SetValue("", targetExe);
                    key?.SetValue("Path", Path.GetDirectoryName(targetExe) ?? "");
                }
                catch { }

                // 7. Register in Windows "Installed Apps" / "Add or remove programs"
                RegisterWindowsUninstaller(targetExe);

                return (true, "AirGlide Remote has been registered on your PC with full Start Menu, Desktop, and Windows Settings 'Installed Apps' support! You can uninstall it anytime.");
            }
            catch (Exception ex)
            {
                return (false, $"Could not configure shortcuts: {ex.Message}");
            }
        }

        public static void RegisterWindowsUninstaller(string? targetExe = null)
        {
            try
            {
                targetExe ??= File.Exists(InstalledExePath) ? InstalledExePath : Process.GetCurrentProcess().MainModule?.FileName ?? "";
                string iconPath = File.Exists(InstalledIconPath) ? InstalledIconPath : targetExe;

                using var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide");
                if (key == null) return;

                key.SetValue("DisplayName", "AirGlide Remote");
                key.SetValue("DisplayVersion", "2.1.0");
                key.SetValue("Publisher", "AirGlide");
                key.SetValue("DisplayIcon", $"{iconPath},0");
                key.SetValue("InstallLocation", InstallFolder);
                key.SetValue("UninstallString", $"\"{targetExe}\" --uninstall");
                key.SetValue("QuietUninstallString", $"\"{targetExe}\" --uninstall --silent");
                key.SetValue("NoModify", 1, RegistryValueKind.DWord);
                key.SetValue("NoRepair", 1, RegistryValueKind.DWord);
                key.SetValue("EstimatedSize", 170000, RegistryValueKind.DWord);
                key.SetValue("URLInfoAbout", "https://github.com/Smilykiller/mouse");
                key.SetValue("InstallDate", DateTime.Now.ToString("yyyyMMdd"));
            }
            catch { }
        }

        public static void UnregisterWindowsUninstaller()
        {
            try
            {
                Registry.CurrentUser.DeleteSubKeyTree(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide", false);
            }
            catch { }
        }

        public static bool PerformUninstall(bool silent = false)
        {
            if (!silent)
            {
                var confirm = MessageBox.Show(
                    "Are you sure you want to completely uninstall AirGlide Remote from this laptop?\n\n" +
                    "This will:\n" +
                    " • Stop and close the laptop receiver\n" +
                    " • Remove Desktop and Start Menu shortcuts\n" +
                    " • Remove automatic Windows startup\n" +
                    " • Remove AirGlide from Windows 'Installed Apps'\n" +
                    " • Delete all application files from AppData",
                    "Uninstall AirGlide Remote",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Question);

                if (confirm != DialogResult.Yes)
                {
                    return false;
                }
            }

            try
            {
                // 1. Disable Auto-Startup
                SetRunAtStartup(false);

                // 2. Remove Windows Registry Keys
                UnregisterWindowsUninstaller();
                try { Registry.CurrentUser.DeleteSubKeyTree(@"Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe", false); } catch { }

                // 3. Remove Desktop & Start Menu Shortcuts
                try { if (File.Exists(DesktopShortcutPath)) File.Delete(DesktopShortcutPath); } catch { }
                try { if (File.Exists(StartMenuShortcutPath)) File.Delete(StartMenuShortcutPath); } catch { }
                try { if (File.Exists(StartMenuUninstallShortcutPath)) File.Delete(StartMenuUninstallShortcutPath); } catch { }

                // 4. Remove Firewall Rule
                try
                {
                    ProcessStartInfo psi = new ProcessStartInfo
                    {
                        FileName = "netsh",
                        Arguments = "advfirewall firewall delete rule name=\"AirGlide UDP\"",
                        CreateNoWindow = true,
                        UseShellExecute = false,
                        WindowStyle = ProcessWindowStyle.Hidden
                    };
                    using var p = Process.Start(psi);
                    p?.WaitForExit(2000);
                }
                catch { }

                // 5. Terminate any other running instances of AirGlide
                int currentPid = Environment.ProcessId;
                foreach (var procName in new[] { "AirGlide", "AirGlide_Laptop_Receiver" })
                {
                    try
                    {
                        foreach (var proc in Process.GetProcessesByName(procName))
                        {
                            if (proc.Id != currentPid)
                            {
                                try { proc.Kill(); } catch { }
                            }
                        }
                    }
                    catch { }
                }

                // 6. Delete install folder via background script
                string cleanupBat = Path.Combine(Path.GetTempPath(), "airglide_uninstall_cleanup.bat");
                string installDir = InstallFolder;
                string script = $@"@echo off
chcp 65001 >nul
timeout /t 1 /nobreak >nul
if exist ""{installDir}"" (
    rd /s /q ""{installDir}"" >nul 2>&1
)
del ""%~f0"" >nul 2>&1
";
                try
                {
                    File.WriteAllText(cleanupBat, script);
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = cleanupBat,
                        CreateNoWindow = true,
                        UseShellExecute = false,
                        WindowStyle = ProcessWindowStyle.Hidden
                    });
                }
                catch { }

                if (!silent)
                {
                    MessageBox.Show(
                        "AirGlide Remote has been completely uninstalled from your PC.\n\nAll shortcuts, startup settings, and application files have been removed.",
                        "AirGlide Remote Uninstalled",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information);
                }

                return true;
            }
            catch (Exception ex)
            {
                if (!silent)
                {
                    MessageBox.Show($"Uninstall encountered an issue: {ex.Message}", "Uninstall AirGlide", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
                return false;
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

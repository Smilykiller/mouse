using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;

namespace AirGlideDesktop
{
    internal static class Program
    {
        private const string SingleInstanceMutexName = @"Local\AirGlideRemote_SingleInstance_Mutex";
        private const string SingleInstanceEventName = @"Local\AirGlideRemote_SingleInstance_Event";

        [DllImport("shell32.dll", SetLastError = true)]
        private static extern int SetCurrentProcessExplicitAppUserModelID([MarshalAs(UnmanagedType.LPWStr)] string AppID);

        [STAThread]
        static void Main(string[] args)
        {
            // 1. Single-Instance Check: Prevent opening multiple duplicate windows/tabs
            bool isFirstInstance = false;
            Mutex? mutex = null;
            try
            {
                mutex = new Mutex(true, SingleInstanceMutexName, out isFirstInstance);
            }
            catch (AbandonedMutexException)
            {
                isFirstInstance = true;
            }

            if (!isFirstInstance)
            {
                // App is already running in another process!
                // Signal the running instance to restore and focus its window, then exit immediately.
                try
                {
                    using var activateEvent = EventWaitHandle.OpenExisting(SingleInstanceEventName);
                    activateEvent.Set();
                }
                catch { }

                return; // Exit second instance immediately - no duplicate window or tab!
            }

            // 2. We are the primary instance: create the signal event
            using var eventHandle = new EventWaitHandle(false, EventResetMode.AutoReset, SingleInstanceEventName);

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
            var mainForm = new MainForm(startMinimized);

            // Register background listener for activation signals from any future duplicate launches
            RegisteredWaitHandle? waitRegistration = null;
            waitRegistration = ThreadPool.RegisterWaitForSingleObject(
                eventHandle,
                (state, timedOut) =>
                {
                    if (!timedOut && mainForm != null && !mainForm.IsDisposed)
                    {
                        try
                        {
                            mainForm.BeginInvoke(new Action(() => mainForm.RestoreWindow()));
                        }
                        catch { }
                    }
                },
                null,
                -1,
                false
            );

            try
            {
                Application.Run(mainForm);
            }
            finally
            {
                waitRegistration?.Unregister(null);
                try { mutex?.ReleaseMutex(); } catch { }
                mutex?.Dispose();
            }
            Environment.Exit(0);
        }
    }
}

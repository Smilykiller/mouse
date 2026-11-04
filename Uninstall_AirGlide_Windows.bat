@echo off
title AirGlide Remote - Windows Uninstaller
echo =========================================================
echo           Uninstalling AirGlide Remote for Windows
echo =========================================================
echo.

echo [1/5] Terminating running AirGlide processes...
taskkill /F /IM AirGlide.exe /IM AirGlide_Laptop_Receiver.exe >nul 2>&1

echo [2/5] Removing Desktop and Start Menu shortcuts...
set "DESKTOP_LNK=%USERPROFILE%\Desktop\AirGlide Remote.lnk"
set "START_LNK=%APPDATA%\Microsoft\Windows\Start Menu\Programs\AirGlide Remote.lnk"
set "START_UNINSTALL_LNK=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Uninstall AirGlide Remote.lnk"
if exist "%DESKTOP_LNK%" del /f /q "%DESKTOP_LNK%" >nul 2>&1
if exist "%START_LNK%" del /f /q "%START_LNK%" >nul 2>&1
if exist "%START_UNINSTALL_LNK%" del /f /q "%START_UNINSTALL_LNK%" >nul 2>&1

echo [3/5] Removing Windows Startup and Registry entries...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "AirGlide Remote" /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "AirGlide" /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe" /f >nul 2>&1
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /f >nul 2>&1

echo [4/5] Removing Windows Firewall rules...
netsh advfirewall firewall delete rule name="AirGlide UDP" >nul 2>&1

echo [5/5] Deleting AirGlide application files...
set "INSTALL_DIR=%LOCALAPPDATA%\AirGlide"
if exist "%INSTALL_DIR%" (
    rd /s /q "%INSTALL_DIR%" >nul 2>&1
)

echo.
echo =========================================================
echo         AirGlide Remote Uninstalled Successfully!
echo =========================================================
echo  * Removed from Start Menu and Desktop
echo  * Removed from Windows "Installed Apps" list
echo  * Removed auto-startup and registry entries
echo  * Cleaned application folder (%LOCALAPPDATA%\AirGlide)
echo =========================================================
echo.
pause

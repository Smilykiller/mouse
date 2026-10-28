@echo off
title Install AirGlide on Laptop
cd /d "%~dp0"

echo ========================================================
echo        AirGlide Desktop Installation & Setup
echo ========================================================
echo.

set "TARGET_EXE=%~dp0AirGlideDesktop.exe"
if not exist "%TARGET_EXE%" (
    set "TARGET_EXE=%~dp0dist\AirGlideDesktop.exe"
)

echo [1/2] Creating Desktop Shortcut: "AirGlide Remote"...
powershell -NoProfile -Command ^
  "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'AirGlide Remote.lnk')); $s.TargetPath = '%TARGET_EXE%'; $s.WorkingDirectory = '%~dp0'; $s.Description = 'AirGlide Laptop Receiver'; $s.Save()"

echo [2/2] Configuring Local Network Firewall Rule for Port 5055 & 5056...
netsh advfirewall firewall add rule name="AirGlide Remote UDP" dir=in action=allow protocol=UDP localport=5055,5056 >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [✓] Firewall rule configured successfully.
) else (
    echo [i] Note: If Windows Firewall prompts you when running, click 'Allow Access'.
)

echo.
echo ========================================================
echo [✓] Setup Complete!
echo Double-click the new shortcut on your Desktop: "AirGlide Remote"
echo ========================================================
echo.
echo Launching AirGlide now...
start "" "%TARGET_EXE%"
exit /b 0

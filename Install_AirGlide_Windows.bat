@echo off
title AirGlide Remote - Windows Setup
echo =========================================================
echo            Installing AirGlide Remote for Windows
echo =========================================================
echo.

set "INSTALL_DIR=%LOCALAPPDATA%\AirGlide"
set "EXE_SRC=%~dp0AirGlide_Laptop_Receiver.exe"
set "ICO_SRC=%~dp0Resources\app_icon.ico"
set "RES_SRC=%~dp0Resources"

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
if not exist "%INSTALL_DIR%\Resources" mkdir "%INSTALL_DIR%\Resources"

echo [1/4] Copying AirGlide application files...
copy /Y "%EXE_SRC%" "%INSTALL_DIR%\AirGlide.exe" >nul
if exist "%ICO_SRC%" copy /Y "%ICO_SRC%" "%INSTALL_DIR%\AirGlide.ico" >nul
if exist "%RES_SRC%" xcopy /E /I /Y "%RES_SRC%" "%INSTALL_DIR%\Resources\" >nul

echo [2/4] Creating Start Menu shortcut with custom logo...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$s = $ws.CreateShortcut([System.IO.Path]::Combine($env:APPDATA, 'Microsoft\Windows\Start Menu\Programs\AirGlide Remote.lnk')); " ^
  "$s.TargetPath = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.exe'); " ^
  "$s.WorkingDirectory = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide'); " ^
  "$s.IconLocation = ([System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.ico') + ',0'); " ^
  "$s.Description = 'AirGlide Remote - Wireless Mouse & Keyboard'; " ^
  "$s.Save()"

echo [3/4] Creating Desktop shortcut...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$s = $ws.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'AirGlide Remote.lnk')); " ^
  "$s.TargetPath = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.exe'); " ^
  "$s.WorkingDirectory = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide'); " ^
  "$s.IconLocation = ([System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.ico') + ',0'); " ^
  "$s.Description = 'AirGlide Remote'; " ^
  "$s.Save()"

echo [4/4] Registering Windows App Path...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe" /ve /t REG_SZ /d "%INSTALL_DIR%\AirGlide.exe" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe" /v Path /t REG_SZ /d "%INSTALL_DIR%" /f >nul 2>&1

echo.
echo =========================================================
echo              Installation Completed Successfully!
echo =========================================================
echo  * Added to Start Menu: Search 'AirGlide Remote' in Start
echo  * Added to Desktop: Shortcut 'AirGlide Remote' created
echo  * To Pin to Taskbar:
echo    Search 'AirGlide' in Start, right-click and click 'Pin to taskbar'
echo =========================================================
echo.

start "" "%INSTALL_DIR%\AirGlide.exe"
pause

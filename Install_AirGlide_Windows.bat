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
set "UNINSTALL_BAT_SRC=%~dp0Uninstall_AirGlide_Windows.bat"

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
if not exist "%INSTALL_DIR%\Resources" mkdir "%INSTALL_DIR%\Resources"

echo [1/5] Copying AirGlide application files...
copy /Y "%EXE_SRC%" "%INSTALL_DIR%\AirGlide.exe" >nul
if exist "%ICO_SRC%" copy /Y "%ICO_SRC%" "%INSTALL_DIR%\AirGlide.ico" >nul
if exist "%RES_SRC%" xcopy /E /I /Y "%RES_SRC%" "%INSTALL_DIR%\Resources\" >nul
if exist "%UNINSTALL_BAT_SRC%" copy /Y "%UNINSTALL_BAT_SRC%" "%INSTALL_DIR%\Uninstall.bat" >nul

echo [2/5] Creating Start Menu shortcut with custom logo...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$s = $ws.CreateShortcut([System.IO.Path]::Combine($env:APPDATA, 'Microsoft\Windows\Start Menu\Programs\AirGlide Remote.lnk')); " ^
  "$s.TargetPath = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.exe'); " ^
  "$s.WorkingDirectory = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide'); " ^
  "$s.IconLocation = ([System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.ico') + ',0'); " ^
  "$s.Description = 'AirGlide Remote - Wireless Mouse & Keyboard'; " ^
  "$s.Save()"

echo [3/5] Creating Desktop shortcut...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$s = $ws.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'AirGlide Remote.lnk')); " ^
  "$s.TargetPath = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.exe'); " ^
  "$s.WorkingDirectory = [System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide'); " ^
  "$s.IconLocation = ([System.IO.Path]::Combine($env:LOCALAPPDATA, 'AirGlide\AirGlide.ico') + ',0'); " ^
  "$s.Description = 'AirGlide Remote'; " ^
  "$s.Save()"

echo [4/5] Registering Windows App Path...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe" /ve /t REG_SZ /d "%INSTALL_DIR%\AirGlide.exe" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths\airglide.exe" /v Path /t REG_SZ /d "%INSTALL_DIR%" /f >nul 2>&1

echo [5/5] Registering in Windows 'Installed Apps' (Settings / Control Panel)...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v DisplayName /t REG_SZ /d "AirGlide Remote" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v DisplayVersion /t REG_SZ /d "2.1.0" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v Publisher /t REG_SZ /d "AirGlide" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v DisplayIcon /t REG_SZ /d "%INSTALL_DIR%\AirGlide.ico,0" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v InstallLocation /t REG_SZ /d "%INSTALL_DIR%" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v UninstallString /t REG_SZ /d "\"%INSTALL_DIR%\AirGlide.exe\" --uninstall" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v QuietUninstallString /t REG_SZ /d "\"%INSTALL_DIR%\AirGlide.exe\" --uninstall --silent" /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v NoModify /t REG_DWORD /d 1 /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v NoRepair /t REG_DWORD /d 1 /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v EstimatedSize /t REG_DWORD /d 170000 /f >nul 2>&1
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\AirGlide" /v URLInfoAbout /t REG_SZ /d "https://github.com/Smilykiller/mouse" /f >nul 2>&1

echo.
echo =========================================================
echo              Installation Completed Successfully!
echo =========================================================
echo  * Added to Start Menu: Search 'AirGlide Remote' in Start
echo  * Added to Desktop: Shortcut 'AirGlide Remote' created
echo  * Registered in Windows Settings: Appears in 'Installed Apps'
echo  * Clean Uninstall: Windows Settings -^> Installed Apps -^> Uninstall,
echo    or run Uninstall_AirGlide_Windows.bat anytime!
echo =========================================================
echo.

start "" "%INSTALL_DIR%\AirGlide.exe"
pause

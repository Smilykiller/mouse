@echo off
title AirGlide Laptop Receiver
cd /d "%~dp0"

echo ========================================================
echo        AirGlide Laptop Receiver Launcher
echo ========================================================
echo.

if exist "%~dp0AirGlideDesktop.exe" (
    echo Starting AirGlide Native Standalone Application...
    start "" "%~dp0AirGlideDesktop.exe"
    exit /b 0
)

if exist "%~dp0dist\AirGlideDesktop.exe" (
    echo Starting AirGlide from dist...
    start "" "%~dp0dist\AirGlideDesktop.exe"
    exit /b 0
)

if exist "%~dp0publish\AirGlideDesktop.exe" (
    echo Starting AirGlide from publish...
    cd /d "%~dp0publish"
    start "" "AirGlideDesktop.exe"
    exit /b 0
)

echo Starting AirGlide Python GUI fallback...
where py >nul 2>nul
if %ERRORLEVEL% equ 0 (
    start "" py "%~dp0airglide_desktop_gui.py"
    exit /b 0
)

where python >nul 2>nul
if %ERRORLEVEL% equ 0 (
    start "" python "%~dp0airglide_desktop_gui.py"
    exit /b 0
)

echo [ERROR] Could not start AirGlide. Please double-click AirGlideDesktop.exe directly.
pause

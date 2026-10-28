@echo off
title AirGlide Laptop Receiver
cd /d "%~dp0"

if exist "%~dp0AirGlideDesktop.exe" (
    start "" "%~dp0AirGlideDesktop.exe"
    exit /b 0
)
if exist "%~dp0publish\AirGlideDesktop.exe" (
    start "" "%~dp0publish\AirGlideDesktop.exe"
    exit /b 0
)

where py >nul 2>nul
if %ERRORLEVEL% equ 0 (
    py "%~dp0airglide_receiver.py"
    goto :done
)
where python >nul 2>nul
if %ERRORLEVEL% equ 0 (
    python "%~dp0airglide_receiver.py"
    goto :done
)

echo [ERROR] Python is not installed. Install it from https://www.python.org/downloads/
echo         (tick "Add python.exe to PATH"), then double-click this file again.
:done
pause

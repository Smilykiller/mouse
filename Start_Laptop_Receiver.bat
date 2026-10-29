@echo off
title AirGlide Laptop Receiver
cd /d "%~dp0"

if exist "%~dp0AirGlide_Laptop_Receiver.exe" (
    start "" "%~dp0AirGlide_Laptop_Receiver.exe"
    exit /b 0
)
if exist "%LOCALAPPDATA%\AirGlide\AirGlide.exe" (
    start "" "%LOCALAPPDATA%\AirGlide\AirGlide.exe"
    exit /b 0
)

where py >nul 2>nul
if %ERRORLEVEL% equ 0 (
    py "%~dp0desktop_receiver\airglide_receiver.py"
    goto :done
)
where python >nul 2>nul
if %ERRORLEVEL% equ 0 (
    python "%~dp0desktop_receiver\airglide_receiver.py"
    goto :done
)

echo [ERROR] AirGlide receiver executable not found.
echo         Please run Install_AirGlide_Windows.bat first.
:done
pause

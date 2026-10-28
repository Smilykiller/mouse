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

cd /d "%~dp0desktop_receiver"
call Start_AirGlide_Laptop.bat

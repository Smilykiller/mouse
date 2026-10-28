@echo off
title AirGlide Laptop Receiver
cd /d "%~dp0"

if exist "%~dp0AirGlide_Laptop_Receiver.exe" (
    start "" "%~dp0AirGlide_Laptop_Receiver.exe"
    exit /b 0
)

if exist "%~dp0desktop_receiver\AirGlideDesktop.exe" (
    start "" "%~dp0desktop_receiver\AirGlideDesktop.exe"
    exit /b 0
)

start "" "%~dp0desktop_receiver\Start_AirGlide_Laptop.bat"
exit /b 0

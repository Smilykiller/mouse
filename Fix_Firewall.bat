@echo off
title AirGlide - Allow Through Firewall
cd /d "%~dp0"
net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)
netsh advfirewall firewall delete rule name="AirGlide UDP" >nul 2>&1
netsh advfirewall firewall delete rule name="AirGlide TCP" >nul 2>&1
netsh advfirewall firewall add rule name="AirGlide UDP" dir=in action=allow protocol=UDP localport=5055,5056 profile=private,domain
netsh advfirewall firewall add rule name="AirGlide TCP" dir=in action=allow protocol=TCP localport=5056 profile=private,domain
echo.
echo =========================================================
echo  Firewall rules added for UDP 5055, 5056 and TCP 5056 (Private/Domain only).
echo  AirGlide Remote is allowed through Windows Firewall.
echo =========================================================
pause

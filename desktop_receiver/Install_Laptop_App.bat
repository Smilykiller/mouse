@echo off
title AirGlide - allow through firewall
cd /d "%~dp0"
net session >nul 2>&1
if %errorlevel% neq 0 (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)
netsh advfirewall firewall delete rule name="AirGlide UDP" >nul 2>&1
netsh advfirewall firewall add rule name="AirGlide UDP" dir=in action=allow protocol=UDP localport=5055,5056 profile=private,public,domain
echo.
echo Firewall rule added for UDP ports 5055 and 5056.
pause

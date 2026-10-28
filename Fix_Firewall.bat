@echo off
:: Self-elevate to Administrator
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo [AirGlide] Requesting administrative privileges to update Windows Firewall...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /B
)

cd /d "%~dp0"
title AirGlide Firewall & Network Setup
color 0A

echo ========================================================
echo    AirGlide Windows Firewall Auto-Configurator
echo ========================================================
echo.

echo [1/4] Deleting old / blocking AirGlide firewall rules...
netsh advfirewall firewall delete rule name="AirGlideDesktop" >nul 2>&1
netsh advfirewall firewall delete rule name="AirGlide Receiver UDP" >nul 2>&1
netsh advfirewall firewall delete rule name="AirGlide Receiver TCP" >nul 2>&1
netsh advfirewall firewall delete rule name="AirGlide Receiver App" >nul 2>&1

echo [2/4] Adding Inbound Port 5055 and 5056 Allow Rules (UDP + TCP, All Profiles)...
netsh advfirewall firewall add rule name="AirGlide Receiver UDP" dir=in action=allow protocol=UDP localport=5055,5056 profile=any >nul
netsh advfirewall firewall add rule name="AirGlide Receiver TCP" dir=in action=allow protocol=TCP localport=5055,5056 profile=any >nul

echo [3/4] Adding Executable Allow Rules...
netsh advfirewall firewall add rule name="AirGlide Receiver App" dir=in action=allow program="%~dp0AirGlide_Laptop_Receiver.exe" profile=any >nul
netsh advfirewall firewall add rule name="AirGlide Receiver App" dir=in action=allow program="%~dp0desktop_receiver\AirGlideDesktop.exe" profile=any >nul

echo [4/4] Enabling local firewall rules on Public and Private profiles...
netsh advfirewall set currentprofile settings localfirewallrules enable >nul
netsh advfirewall set publicprofile settings localfirewallrules enable >nul
netsh advfirewall set privateprofile settings localfirewallrules enable >nul

echo.
echo ========================================================
echo [SUCCESS] Windows Firewall is now 100%% configured!
echo Inbound UDP ports 5055/5056 are now OPEN for your phone.
echo ========================================================
echo.
pause

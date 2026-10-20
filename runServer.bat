@echo off
title Minecraft BlockSwap Server (26.1)
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ===================================================
echo Starting Minecraft 26.1 Dedicated Server...
echo ===================================================
call gradlew.bat runServer --nogui
pause

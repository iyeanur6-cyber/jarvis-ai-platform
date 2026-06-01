@echo off
REM Jarvis AI Platform Launcher for Windows
set JARVIS_DIR=%~dp0
set JAR=%JARVIS_DIR%jarvis-server-0.1.0.jar

if not exist "%JAR%" (
    echo.
    echo Jarvis JAR not found.
    echo Download from: https://github.com/sujankim/jarvis-ai-platform/releases
    echo Place jarvis-server-0.1.0.jar in the same folder as this script.
    pause
    exit /b 1
)

echo Starting Jarvis AI Platform...
java -jar "%JAR%" %*
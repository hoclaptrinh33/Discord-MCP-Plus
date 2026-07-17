@echo off
echo ========================================
echo   Deploy Discord MCP Fork (Admin)
echo ========================================
echo.

REM Stop HermesGateway service
echo [1/4] Stopping HermesGateway service...
nssm stop HermesGateway 2>nul
timeout /t 2 >nul

REM Check Java
echo [2/4] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found!
    pause
    exit /b 1
)

REM Check JAR
if not exist "target\discord-mcp-1.0.0.jar" (
    echo [ERROR] JAR not found!
    pause
    exit /b 1
)

REM Set env and run
echo [3/4] Setting Discord token...
set DISCORD_TOKEN=MTUy...xrxc
set SPRING_PROFILES_ACTIVE=http

echo [4/4] Starting Discord MCP Fork...
echo.
java -jar target\discord-mcp-1.0.0.jar

pause

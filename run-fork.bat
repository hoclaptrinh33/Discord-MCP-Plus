@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo   Discord MCP Fork - Windows Launcher
echo ========================================
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JDK 17+.
    pause
    exit /b 1
)

REM Check JAR
if not exist "target\discord-mcp-1.0.0.jar" (
    echo [ERROR] JAR not found: target\discord-mcp-1.0.0.jar
    echo Run: mvn clean package -DskipTests
    pause
    exit /b 1
)

REM Set token from env or prompt
if "%DISCORD_TOKEN%"=="" (
    echo [INFO] DISCORD_TOKEN not set in environment.
    echo Please set it first:
    echo    set DISCORD_TOKEN=your_token_here
    echo Or edit this file to hardcode.
    pause
    exit /b 1
)

echo [OK] Starting Discord MCP Fork...
echo [INFO] Token: %DISCORD_TOKEN:~0,10%...
echo [INFO] Profile: http
echo.

set SPRING_PROFILES_ACTIVE=http
java -jar target\discord-mcp-1.0.0.jar

pause
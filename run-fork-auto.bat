@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo   Discord MCP Fork - Auto Deploy
echo ========================================
echo.

set DISCORD_TOKEN=%DISCORD_TOKEN%
set SPRING_PROFILES_ACTIVE=http

echo [INFO] Token: %DISCORD_TOKEN:~0,20%...
echo [INFO] Profile: http
echo [INFO] Starting...
echo.

java -jar target\discord-mcp-1.0.0.jar

pause
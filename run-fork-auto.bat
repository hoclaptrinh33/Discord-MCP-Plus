@echo off
setlocal EnableDelayedExpansion

echo ========================================
echo   Discord MCP Fork - Auto Deploy
echo ========================================
echo.

set "DISCORD_TOKEN=%DISCORD_TOKEN%"
set SPRING_PROFILES_ACTIVE=http

if defined DISCORD_TOKEN (
  echo [INFO] Discord token configured from environment.
) else (
  echo [INFO] Discord token not set. Provide it via environment variable or .env.
)

echo [INFO] Profile: http
echo [INFO] Starting...
echo.

java -jar target\discord-mcp-1.0.0.jar

pause
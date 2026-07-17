@echo off
chcp 65001 >nul
echo ============================================
echo   WATCH FORK LOG (realtime)
echo ============================================
echo.
echo Press Ctrl+C to stop watching.
echo.
powershell -Command "Get-Content fork.log -Wait -Tail 30"
pause
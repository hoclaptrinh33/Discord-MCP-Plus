@echo off
chcp 65001 >nul
echo ============================================
echo   DEPLOY DISCORD MCP FORK - MINECRAFT ROLE
echo ============================================
echo.

:: 1. Kill process cũ
echo [1/4] Killing old fork process...
for /f "tokens=2" %%a in ('tasklist ^| findstr /i "java.exe"') do (
    wmic process where "ProcessId=%%a" get CommandLine 2>nul | findstr /i "discord-mcp" >nul && (
        echo   Killing PID %%a
        taskkill /F /PID %%a >nul 2>&1
    )
)
timeout /t 2 >nul

:: 2. Clean + Build
echo [2/4] Building JAR (mvn clean package)...
call ./mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed!
    pause
    exit /b 1
)
echo   Build SUCCESS: target\discord-mcp-1.0.0.jar

:: 3. Start fork with logging
echo [3/4] Starting fork on port 8085 (log to fork.log)...
if exist fork.log del /Q fork.log
start "DiscordMCP-Fork" cmd /c "java -jar target\discord-mcp-1.0.0.jar > fork.log 2>&1"

:: 4. Wait login
echo [4/4] Waiting for login (15s)...
timeout /t 15 >nul

echo.
echo ============================================
echo   DEPLOY COMPLETE
echo ============================================
echo.
echo Guild: 1355412399464910978 (Thiên Nguyên GIới)
echo Role Minecraft: 1522888351336890408 (⛏️ Minecraft)
echo.
echo Log file: fork.log (use: type fork.log or tail -f)
echo.
echo Test buttons:
echo   - custom_id: "role_add:1522888351336890408"
echo   - custom_id: "role_toggle:1522888351336890408"
echo.
echo To watch logs realtime:
echo   powershell -Command "Get-Content fork.log -Wait -Tail 20"
echo.
pause
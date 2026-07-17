#!/bin/bash
export DISCORD_TOKEN="${DISCORD_TOKEN:-YOUR_DISCORD_BOT_TOKEN}"
export SPRING_PROFILES_ACTIVE=http
echo "Starting Discord MCP Fork with token: ${DISCORD_TOKEN:0:20}..."
exec java -jar target/discord-mcp-1.0.0.jar

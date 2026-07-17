#!/bin/bash
# Test Minecraft Role Buttons - Discord MCP Fork

GUILD_ID="1355412399464910978"
ROLE_MINECRAFT="1522888351336890408"
CHANNEL_BOT="1522888351336890408"  # TODO: thay bằng channel #bot-commands thật

echo "============================================"
echo "  TEST MINECRAFT ROLE BUTTONS"
echo "============================================"
echo ""
echo "Guild: $GUILD_ID"
echo "Role ⛏️ Minecraft: $ROLE_MINECRAFT"
echo ""

# Test 1: Button role_add
echo "[TEST 1] Button: role_add (add Minecraft role)"
curl -s -X POST http://localhost:8085/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "send_message",
      "arguments": {
        "channelId": "'$CHANNEL_BOT'",
        "message": "🎮 **Test Role Minecraft**\nClick button để nhận role ⛏️ Minecraft",
        "embedsJson": "[{\"title\":\"Minecraft Role Test\",\"description\":\"Nhận role Minecraft để tham gia server\",\"color\":3447003}]",
        "componentsJson": "[{\"type\":1,\"components\":[{\"type\":2,\"style\":1,\"label\":\"Nhận Role ⛏️\",\"custom_id\":\"role_add:'$ROLE_MINECRAFT'\",\"emoji\":{\"name\":\"⛏️\"}}]}]"
      }
    }
  }' | jq '.result.content[0].text'

echo ""
echo "[TEST 2] Button: role_toggle (toggle Minecraft role)"
curl -s -X POST http://localhost:8085/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "send_message",
      "arguments": {
        "channelId": "'$CHANNEL_BOT'",
        "message": "🔄 **Toggle Role Minecraft**\nClick để thêm/xóa role ⛏️ Minecraft",
        "componentsJson": "[{\"type\":1,\"components\":[{\"type\":2,\"style\":2,\"label\":\"Toggle ⛏️ Minecraft\",\"custom_id\":\"role_toggle:'$ROLE_MINECRAFT'\"}]}]"
      }
    }
  }' | jq '.result.content[0].text'

echo ""
echo "============================================"
echo "  DONE - Check Discord channel"
echo "============================================"
echo ""
echo "Expected behavior:"
echo "  - Button 'Nhận Role ⛏️' → User click → Bot add role Minecraft"
echo "  - Button 'Toggle ⛏️ Minecraft' → User click → Bot add/remove role"
echo ""
echo "Note: Nếu user đã có role → Bot báo 'Bạn đã có role này rồi'"
echo "      Nếu user chưa có → Bot add role + reply success"
echo ""
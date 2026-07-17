#!/bin/bash
# Test Discord MCP Fork - Phase 1+2 Features
# Run after fork is live on localhost:8085

MCP_URL="http://localhost:8085/mcp"
CHANNEL_ID="1522800654442168461"

echo "=== Test 1: Embed ==="
curl -s -X POST $MCP_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "id":1,
    "method":"tools/call",
    "params":{
      "name":"send_message",
      "arguments":{
        "channelId":"'$CHANNEL_ID'",
        "message":"Test Embed",
        "embedsJson":"[{\"title\":\"Test Embed\",\"description\":\"Embed từ fork\",\"color\":65280}]"
      }
    }
  }' | jq -r '.result.content[0].text // .error.message'

echo ""
echo "=== Test 2: Button ==="
curl -s -X POST $MCP_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "id":2,
    "method":"tools/call",
    "params":{
      "name":"send_message",
      "arguments":{
        "channelId":"'$CHANNEL_ID'",
        "message":"Test Button",
        "componentsJson":"[{\"components\":[{\"type\":2,\"style\":1,\"label\":\"Click Me\",\"custom_id\":\"btn_test\"}]}]"
      }
    }
  }' | jq -r '.result.content[0].text // .error.message'

echo ""
echo "=== Test 3: Select Menu ==="
curl -s -X POST $MCP_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "id":3,
    "method":"tools/call",
    "params":{
      "name":"send_message",
      "arguments":{
        "channelId":"'$CHANNEL_ID'",
        "message":"Test Select Menu",
        "componentsJson":"[{\"components\":[{\"type\":3,\"custom_id\":\"menu_test\",\"placeholder\":\"Chọn\",\"options\":[{\"label\":\"Option A\",\"value\":\"a\"},{\"label\":\"Option B\",\"value\":\"b\"}]}]}]"
      }
    }
  }' | jq -r '.result.content[0].text // .error.message'

echo ""
echo "=== Done ==="
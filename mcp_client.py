#!/usr/bin/env python3
"""Minimal MCP Streamable-HTTP client for the local Discord MCP server."""
import json
import sys
import urllib.request
import urllib.error

URL = "http://localhost:8085/mcp"
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
}


def _parse_sse(body):
    """Extract the JSON-RPC result from an SSE or plain JSON body."""
    body = body.strip()
    if body.startswith("event:") or "data:" in body:
        # SSE format: lines "data: {...}"
        for line in body.splitlines():
            line = line.strip()
            if line.startswith("data:"):
                data = line[len("data:"):].strip()
                if data and data != "[DONE]":
                    try:
                        return json.loads(data)
                    except json.JSONDecodeError:
                        continue
        return None
    try:
        return json.loads(body)
    except json.JSONDecodeError:
        return None


class McpClient:
    def __init__(self):
        self.session_id = None

    def _post(self, payload, expect_response=True):
        data = json.dumps(payload).encode()
        headers = dict(HEADERS)
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        req = urllib.request.Request(URL, data=data, headers=headers, method="POST")
        try:
            resp = urllib.request.urlopen(req, timeout=60)
        except urllib.error.HTTPError as e:
            print(f"HTTPError {e.code}: {e.read().decode()[:500]}", file=sys.stderr)
            raise
        if "Mcp-Session-Id" in resp.headers:
            self.session_id = resp.headers["Mcp-Session-Id"]
        body = resp.read().decode()
        if not expect_response:
            return None
        return _parse_sse(body)

    def initialize(self):
        res = self._post({
            "jsonrpc": "2.0", "id": 1, "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "hermes-cli", "version": "1.0"},
            },
        })
        # send initialized notification
        self._post({
            "jsonrpc": "2.0", "method": "notifications/initialized", "params": {}
        }, expect_response=False)
        return res

    def list_tools(self):
        return self._post({
            "jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}
        })

    def call_tool(self, name, arguments):
        return self._post({
            "jsonrpc": "2.0", "id": 3, "method": "tools/call",
            "params": {"name": name, "arguments": arguments},
        })


def main():
    client = McpClient()
    client.initialize()
    if len(sys.argv) == 1 or sys.argv[1] == "list":
        res = client.list_tools()
        tools = res.get("result", {}).get("tools", [])
        print(f"TOOL COUNT: {len(tools)}")
        for t in tools:
            print("-", t.get("name"))
        return
    # call mode: argv = toolname json_args
    name = sys.argv[1]
    args = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}
    res = client.call_tool(name, args)
    print(json.dumps(res, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()

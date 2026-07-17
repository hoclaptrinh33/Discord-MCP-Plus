#!/usr/bin/env python3
"""Clear message history from specified Discord channels (keep channels).
Uses Discord REST API directly with the bot token for efficient bulk-delete.
Messages <14 days -> bulk-delete (100/req). Older -> individual delete (rate-limited).
"""
import json, time, urllib.request, urllib.error, datetime

# --- load token from .env.new ---
TOKEN = None
with open(r'C:\Users\lehai\discord-mcp-fork\.env.new') as f:
    for line in f:
        line = line.strip()
        if line.startswith('DISCORD_TOKEN='):
            TOKEN = line.split('=', 1)[1].strip()
assert TOKEN, "token not found"

BASE = "https://discord.com/api/v10"
HDR = {"Authorization": "Bot " + TOKEN, "Content-Type": "application/json",
       "User-Agent": "hermes-clear/1.0"}

# channels to clear (exclude THÔNG TIN info board + THẢO LUẬN keep-as-is)
CHANNELS = {
    'hermes-chat': '1522800654442168461',
    'admin-chat': '1522883289428398223',
    'cấu-hình-bot': '1522883321112297603',
    'analytics-log': '1522883337495253154',
    'trạng-thái-server': '1522882452128010421',
    'danh-sách-whitelist': '1522882543240745061',
    'báo-lỗi-đề-xuất': '1522882571854151681',
    'event-thành-tựu': '1522882646961819818',
    'đăng-ký-whitelist': '1522882699059003442',
    'minecraft-info': '1522903502622429246',
    'minecraft-chat': '1522903548461973654',
    'nhật-ký-mod': '1522882826725228594',
    'ticket-hỗ-trợ': '1522882869494677634',
    'bot-commands': '1522882895574863913',
    'valoran': '1522888625828659242',
    'liên-minh-huyền-thoại': '1522888702014128310',
    'đấu-trường-chân-lý': '1522888848739139685',
    'ticket-support': '1524324016800923678',
}

FOURTEEN_DAYS = 14 * 24 * 3600
now = datetime.datetime.now(datetime.timezone.utc)

def api(method, url, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + url, data=data, headers=HDR, method=method)
    try:
        resp = urllib.request.urlopen(req, timeout=60)
        return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

def is_old(ts):
    # ts like 2026-07-08T12:00:00.000000+00:00
    try:
        dt = datetime.datetime.fromisoformat(ts.replace('Z', '+00:00'))
        return (now - dt).total_seconds() > FOURTEEN_DAYS
    except Exception:
        return False

def clear_channel(name, cid):
    total = 0
    while True:
        st, body = api("GET", f"/channels/{cid}/messages?limit=100")
        if st != 200:
            print(f"  [{name}] GET err {st}: {body[:120]}")
            break
        msgs = json.loads(body)
        if not msgs:
            break
        new_ids = [m["id"] for m in msgs if not is_old(m.get("timestamp", ""))]
        old_ids = [m["id"] for m in msgs if is_old(m.get("timestamp", ""))]
        if new_ids:
            st2, b2 = api("POST", f"/channels/{cid}/messages/bulk-delete",
                          {"messages": new_ids})
            if st2 == 204:
                total += len(new_ids)
            elif st2 == 429:
                ra = json.loads(b2).get("retry_after", 1)
                print(f"  [{name}] rate-limited, sleep {ra}s")
                time.sleep(ra + 1)
                continue
            else:
                print(f"  [{name}] bulk-del err {st2}: {b2[:120]}")
        for oid in old_ids:
            st3, b3 = api("DELETE", f"/channels/{cid}/messages/{oid}")
            if st3 in (200, 204):
                total += 1
            elif st3 == 429:
                ra = json.loads(b3).get("retry_after", 1)
                time.sleep(ra + 1)
                # retry this one
                st3, b3 = api("DELETE", f"/channels/{cid}/messages/{oid}")
                if st3 in (200, 204):
                    total += 1
            time.sleep(1.0)
        time.sleep(1.0)
    print(f"CLEARED {name}: {total} messages")
    return total

if __name__ == "__main__":
    grand = 0
    for name, cid in CHANNELS.items():
        print(f"--> {name}")
        grand += clear_channel(name, cid)
    print(f"\n=== DONE. Total messages deleted: {grand} ===")

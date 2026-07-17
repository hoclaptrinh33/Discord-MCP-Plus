import mcp_client, json, time
c = mcp_client.McpClient(); c.initialize()
G = '1355412399464910978'
EVERYONE = G
THONG_TIN_KIDS = {
    'luật-server': '1522637713122132160',
    'chào-mừng': '1522637716200755352',
    'thông-báo': '1522637719421845624',
    'nhận-role': '1522637721858871468',
    'thông-tin-server': '1522883037187018883',
    'liên-kết': '1522883073006370917',
}
for name, cid in THONG_TIN_KIDS.items():
    r = c.call_tool('delete_channel_permission_overwrite', {
        'guildId': G, 'channelId': cid, 'targetType': 'role',
        'targetId': EVERYONE, 'reason': 'Remove child overwrite to inherit THÔNG TIN read-only category',
    })
    txt = r.get('result', {}).get('content', [{}])[0].get('text', '')
    print(f'{name}: {txt[:90]}')
    time.sleep(0.5)

print('\n=== VERIFY (should be 0 overwrites each) ===')
for name, cid in THONG_TIN_KIDS.items():
    ov = c.call_tool('list_channel_permission_overwrites', {'guildId': G, 'channelId': cid})
    txt = ov.get('result', {}).get('content', [{}])[0].get('text', '')
    print(f'{name}: {txt[:80]}')

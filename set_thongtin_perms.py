import mcp_client, json
c = mcp_client.McpClient(); c.initialize()
G = '1355412399464910978'
EVERYONE = G  # @everyone role id == guild id
THONG_TIN = '1522637707233460244'  # THÔNG TIN category id

# Make THÔNG TIN read-only for @everyone:
#  allowRaw = 1024 (VIEW_CHANNEL)
#  denyRaw  = 2048 (SEND_MESSAGES)
res = c.call_tool('upsert_role_channel_permissions', {
    'guildId': G,
    'channelId': THONG_TIN,
    'roleId': EVERYONE,
    'allowRaw': '1024',
    'denyRaw': '2048',
    'reason': 'THÔNG TIN read-only: @everyone can view but not send',
})
print('UPSERT RESULT:')
print(json.dumps(res.get('result', {}), ensure_ascii=False)[:800])
print('isError:', res.get('result', {}).get('isError'))

# Verify
ov = c.call_tool('list_channel_permission_overwrites', {
    'guildId': G,
    'channelId': THONG_TIN,
})
print('\nOVERWRITES ON THÔNG TIN:')
print(json.dumps(ov.get('result', {}), ensure_ascii=False)[:1200])

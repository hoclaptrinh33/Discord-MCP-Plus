# Discord MCP Fork - Tool Schema & Documentation

**Version:** 1.0.0
**Status:** Phase 1 + 2 complete (text, embed, button, select, file, edit, interaction, permission, audit)

## Available Tools (97 total)

### Message Tools

#### send_message
Gửi tin nhắn (text + embed + button/select + file).

**Parameters:**
- `channelId` (required): Discord channel ID
- `message` (optional): Nội dung text
- `embedsJson` (optional): JSON array embed objects
- `componentsJson` (optional): JSON array ActionRow. **QUAN TRỌNG:** Dùng `custom_id` (snake_case), KHÔNG dùng `customId`
- `filesJson` (optional): JSON array file objects (path, base64, hoặc URL)

**Example button:**
```json
[{"type":1,"components":[{"type":2,"style":1,"label":"Click","custom_id":"btn_1","emoji":{"name":"🔵"}}]}]
```

**Example select menu:**
```json
[{"type":1,"components":[{"type":3,"custom_id":"menu_1","placeholder":"Chọn...","options":[{"label":"Opt1","value":"v1","description":"Mô tả"}]}]}]
```

**Example file (path):**
```json
[{"path":"C:\\Users\\lehai\\test.png","filename":"test.png"}]
```

#### edit_message
Sửa tin nhắn đã gửi.

**Parameters:**
- `channelId`, `messageId` (required)
- `newMessage`, `embedsJson`, `componentsJson` (optional)

#### delete_message
Xóa tin nhắn.

#### read_messages
Đọc lịch sử tin nhắn (before/after/around).

### Interaction Tools

#### list_pending_interactions
Liệt kê interaction đang chờ (button/select/slash click).

**Parameters:**
- `limit` (optional, default 50)

#### get_interaction
Lấy chi tiết 1 interaction.

**Parameters:**
- `token` (required)

#### respond_interaction
Trả lời interaction (gửi message/embed/button).

**Parameters:**
- `token` (required)
- `content`, `embedsJson`, `componentsJson`, `ephemeral` (optional)

#### defer_interaction
Trì hoãn trả lời (acknowledge, sau đó có 15 phút để respond).

#### respond_with_modal
Mở modal form.

### Permission & Audit Tools

#### check_user_permission
Kiểm tra quyền user.

**Parameters:**
- `guildId`, `userId`, `permission` (required)
- `roleId` (optional)

#### get_audit_logs
Lấy audit log server.

**Parameters:**
- `guildId` (required)
- `limit` (optional, default 10)

#### register_slash_command
Đăng ký slash command.

**Parameters:**
- `name`, `description` (required)
- `guildId` (optional = global)

### Channel / Role / Member Tools (standard JDA)

- list_channels, create_text_channel, edit_text_channel, delete_channel
- list_roles, create_role, edit_role, delete_role, assign_role, remove_role
- ban_member, kick_member, timeout_member, etc.
- create_thread, archive_thread, lock_thread, etc.

## Schema Notes (QUAN TRỌNG)

1. **Button/Select:** Luôn dùng `custom_id` (snake_case). Tool sẽ throw nếu thiếu.
2. **File:** Ưu tiên `path` (local file). Hỗ trợ base64 + URL.
3. **Embed color:** Dùng số decimal (65280 = xanh lá).
4. **Ephemeral:** Dùng cho respond_interaction (chỉ user thấy).

## Known Issues / Limitations

- File upload qua MCP client chưa test thực tế (cần test thêm).
- Modal response chưa có example đầy đủ.
- Slash command chưa register thực tế.

## Build & Run

```bash
./mvnw clean package -DskipTests
java -jar target/discord-mcp-1.0.0.jar
```

Server lắng nghe: `http://localhost:8085/mcp`

---

**Last updated:** 2026-07-06
**Author:** Hermes Agent + User

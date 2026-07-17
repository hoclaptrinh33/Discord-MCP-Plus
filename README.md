<div align="center">
  <img src="assets/img/Discord_MCP_full_logo.svg" width="60%" alt="Discord MCP Plus" />
</div>
<hr>
<div align="center" style="line-height: 1;">
    <a href="https://github.com/modelcontextprotocol/servers" target="_blank" style="margin: 2px;">
        <img alt="MCP Server" src="https://badge.mcpx.dev?type=server" style="display: inline-block; vertical-align: middle;"/>
    </a>
    <a href="https://github.com/hoclaptrinh33/Discord-MCP-Plus/blob/main/LICENSE" target="_blank" style="margin: 2px;">
        <img alt="MIT License" src="https://img.shields.io/github/license/hoclaptrinh33/Discord-MCP-Plus" style="display: inline-block; vertical-align: middle;"/>
    </a>
    <a href="https://github.com/hoclaptrinh33/Discord-MCP-Plus" target="_blank" style="margin: 2px;">
        <img alt="GitHub Stars" src="https://img.shields.io/github/stars/hoclaptrinh33/Discord-MCP-Plus?style=social" style="display: inline-block; vertical-align: middle;"/>
    </a>
</div>


## 📖 Mô tả

**Discord MCP Plus** là một [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) server mở rộng cho Discord API, xây dựng trên nền tảng [(JDA)](https://jda.wiki/).

Fork này bổ sung đáng kể các tính năng so với bản gốc [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp), tập trung vào:
- 🧵 Quản lý **Thread** nâng cao
- 🎫 Hệ thống **Ticket** hỗ trợ tự động
- 📊 **Poll** (thăm dò ý kiến) native Discord
- 🎭 Hệ thống **Modal** và **Interaction** đầy đủ
- 🔐 Kiểm tra **Permission** và **Audit Log**
- ✅ Tự động đăng ký **Slash Command**

## ✨ Tính năng bổ sung so với bản gốc

| Nhóm | Tool mới |
|------|----------|
| **Interactions** | `list_pending_interactions`, `get_interaction`, `respond_interaction`, `defer_interaction`, `edit_interaction_response`, `followup_interaction`, `delete_interaction_response`, `respond_autocomplete`, `respond_with_modal` |
| **Slash Commands** | `register_slash_command` |
| **Permissions** | `check_user_permission`, `check_bot_permission`, `get_effective_permissions`, `get_audit_logs`, `sync_channel_permissions_with_category`, `sync_all_channels_in_category` |
| **Polls** | `create_poll`, `get_poll_results`, `end_poll` |
| **Threads** | `create_thread`, `create_private_thread`, `get_thread`, `list_threads`, `list_guild_threads`, `archive_thread`, `lock_thread`, `add_thread_member`, `remove_thread_member` |
| **Tickets** | `create_ticket`, `close_ticket`, `list_tickets` |
| **Modals** | `create_modal_payload`, `send_modal`, `respond_modal` |
| **Attachments** | `get_attachment` |
| **Webhooks** | `edit_webhook` (thêm vào bộ webhook) |

## 🔬 Cài đặt

### ► 🐳 Docker (Khuyến nghị)

> [!NOTE]
> Yêu cầu Docker. Hướng dẫn cài đặt tại [docker.com](https://www.docker.com/products/docker-desktop/).

#### 1) Thiết lập biến môi trường
```bash
export DISCORD_TOKEN="YOUR_DISCORD_BOT_TOKEN"
export DISCORD_GUILD_ID="OPTIONAL_DEFAULT_SERVER_ID"
export SPRING_PROFILES_ACTIVE=http
```

> [!IMPORTANT]
> Hướng dẫn tạo Discord bot và lấy token: [discordjs.guide](https://discordjs.guide/legacy/preparations/app-setup)

> [!TIP]
> `DISCORD_GUILD_ID` là tuỳ chọn. Khi cung cấp, các tool có tham số `guildId` có thể bỏ qua tham số đó.

#### 2) Chạy container
```bash
docker run -d -i \
  --name discord-mcp-plus \
  --restart unless-stopped \
  -p 8085:8085 \
  -e SPRING_PROFILES_ACTIVE \
  -e DISCORD_TOKEN \
  -e DISCORD_GUILD_ID \
  ghcr.io/hoclaptrinh33/discord-mcp-plus:latest
```

MCP endpoint: `http://localhost:8085/mcp`

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🐋 Docker Compose
    </summary>

#### 1) Clone repository
```bash
git clone https://github.com/hoclaptrinh33/Discord-MCP-Plus
```

#### 2) Tạo file `.env`
```bash
cat > .env <<EOF
SPRING_PROFILES_ACTIVE=http
DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN>
DISCORD_GUILD_ID=<OPTIONAL_DEFAULT_SERVER_ID>
EOF
```

#### 3) Khởi động container
```bash
docker compose up -d --build
```

#### 4) Kiểm tra
```bash
docker ps --filter name=discord-mcp-plus
curl -fsS http://localhost:8085/actuator/health
```

MCP endpoint: `http://localhost:8085/mcp`

Health endpoint: `http://localhost:8085/actuator/health`

</details>

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🔧 Cài đặt thủ công (Maven)
    </summary>

#### 1) Clone repository
```bash
git clone https://github.com/hoclaptrinh33/Discord-MCP-Plus
cd Discord-MCP-Plus
```

#### 2) Build project
```bash
mvn clean package
```

#### 3) Chạy JAR
```bash
DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN> \
DISCORD_GUILD_ID=<OPTIONAL_DEFAULT_SERVER_ID> \
SPRING_PROFILES_ACTIVE=http \
java -jar target/discord-mcp-*.jar
```

MCP endpoint: `http://localhost:8085/mcp`

</details>

## 🔗 Kết nối

### ► config.json (HTTP)

```json
{
  "mcpServers": {
    "discord-mcp-plus": {
      "url": "http://localhost:8085/mcp"
    }
  }
}
```

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        ⌨️ Claude Code
    </summary>

```bash
claude mcp add discord-mcp-plus --transport http http://localhost:8085/mcp
```

</details>

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🖥 Claude Desktop
    </summary>

```json
{
  "mcpServers": {
    "discord-mcp-plus": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "-e", "DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN>",
        "-e", "DISCORD_GUILD_ID=<OPTIONAL_DEFAULT_SERVER_ID>",
        "ghcr.io/hoclaptrinh33/discord-mcp-plus:latest"
      ]
    }
  }
}
```

</details>

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🖲 Cursor
    </summary>

Vào: `Settings` → `Cursor Settings` → `MCP` → `Add new global MCP server`

```json
{
  "mcpServers": {
    "discord-mcp-plus": {
      "url": "http://localhost:8085/mcp"
    }
  }
}
```

</details>

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🚀 n8n
    </summary>

1. Mở n8n, thêm node **MCP Client**.
2. Chọn transport **HTTP** hoặc **Streamable HTTP**.
3. URL: `http://localhost:8085/mcp`
4. Lưu và test kết nối.

> Nếu n8n chạy trong Docker, dùng service name: `http://discord-mcp-plus:8085/mcp`

</details>


## 🛠️ Danh sách Tool

> Nếu `DISCORD_GUILD_ID` được cấu hình, tham số `guildId` là tuỳ chọn cho tất cả tool.

---

#### Server Information
- [`get_server_info`]() — Lấy thông tin chi tiết về Discord server

---

#### User Management
- [`get_user_id_by_name`]() — Lấy User ID theo username trong server (dùng để ping `<@id>`)
- [`send_private_message`]() — Gửi tin nhắn riêng (DM) tới user
- [`edit_private_message`]() — Chỉnh sửa tin nhắn riêng đã gửi
- [`delete_private_message`]() — Xóa tin nhắn riêng
- [`read_private_messages`]() — Đọc lịch sử DM (hỗ trợ `count` 1-100, con trỏ `before`/`after`/`around`)

---

#### Message Management
- [`send_message`]() — Gửi tin nhắn tới channel (hỗ trợ embed, components, file)
- [`edit_message`]() — Chỉnh sửa tin nhắn trong channel
- [`delete_message`]() — Xóa tin nhắn
- [`read_messages`]() — Đọc lịch sử tin nhắn (hỗ trợ `count` 1-100, con trỏ `before`/`after`/`around`)
- [`add_reaction`]() — Thêm reaction (emoji) vào tin nhắn
- [`remove_reaction`]() — Xóa reaction khỏi tin nhắn
- [`get_attachment`]() — Lấy thông tin và URL của file đính kèm trong tin nhắn

---

#### Channel Management
- [`create_text_channel`]() — Tạo text channel mới
- [`edit_text_channel`]() — Chỉnh sửa cấu hình text channel (tên, topic, nsfw, slowmode, category, position)
- [`delete_channel`]() — Xóa channel
- [`find_channel`]() — Tìm channel theo tên và server ID
- [`list_channels`]() — Liệt kê tất cả channel trong server
- [`get_channel_info`]() — Lấy thông tin chi tiết về channel
- [`move_channel`]() — Di chuyển channel sang category khác hoặc đổi vị trí

---

#### Category Management
- [`create_category`]() — Tạo category mới
- [`edit_category`]() — Đổi tên hoặc vị trí category
- [`delete_category`]() — Xóa category
- [`find_category`]() — Tìm category theo tên
- [`list_channels_in_category`]() — Liệt kê channel trong một category

---

#### Channel Permissions
- [`list_channel_permission_overwrites`]() — Liệt kê tất cả permission overwrite của channel
- [`upsert_role_channel_permissions`]() — Tạo/cập nhật permission overwrite cho role trên channel
- [`upsert_member_channel_permissions`]() — Tạo/cập nhật permission overwrite cho member trên channel
- [`delete_channel_permission_overwrite`]() — Xóa permission overwrite của role/member trên channel
- [`get_effective_permissions`]() — Lấy effective permission của user/role trên một channel cụ thể
- [`sync_channel_permissions_with_category`]() — Đồng bộ permission của channel với category cha
- [`sync_all_channels_in_category`]() — Đồng bộ permission của tất cả channel trong category

---

#### Permission & Audit
- [`check_user_permission`]() — Kiểm tra user có permission cụ thể trong channel/server không
- [`check_bot_permission`]() — Kiểm tra bot có permission cụ thể trong channel/server không
- [`get_audit_logs`]() — Lấy audit log của server (lọc theo action type, user, số lượng)

---

#### Slash Command Management
- [`register_slash_command`]() — Đăng ký slash command mới trong server

---

#### Interaction Management
- [`list_pending_interactions`]() — Liệt kê tất cả pending interactions đang chờ xử lý
- [`get_interaction`]() — Lấy chi tiết của một interaction cụ thể theo token
- [`respond_interaction`]() — Phản hồi interaction với tin nhắn (initial response)
- [`defer_interaction`]() — Defer interaction response (acknowledge không có nội dung, dành cho xử lý lâu)
- [`edit_interaction_response`]() — Chỉnh sửa tin nhắn phản hồi interaction ban đầu
- [`followup_interaction`]() — Gửi tin nhắn followup sau khi defer
- [`delete_interaction_response`]() — Xóa tin nhắn phản hồi interaction
- [`respond_autocomplete`]() — Phản hồi autocomplete interaction với danh sách gợi ý
- [`respond_with_modal`]() — Phản hồi component interaction bằng cách mở modal

---

#### Modal Management
- [`create_modal_payload`]() — Tạo payload modal (title, fields) để dùng với interaction
- [`send_modal`]() — Gửi modal tới user thông qua interaction token
- [`respond_modal`]() — Xử lý phản hồi từ modal submission

---

#### Webhook Management
- [`create_webhook`]() — Tạo webhook mới trên channel
- [`delete_webhook`]() — Xóa webhook
- [`list_webhooks`]() — Liệt kê webhooks trên channel
- [`send_webhook_message`]() — Gửi tin nhắn qua webhook
- [`edit_webhook`]() — Chỉnh sửa webhook (tên, avatar, channel)

---

#### Role Management
- [`list_roles`]() — Liệt kê tất cả role trong server
- [`create_role`]() — Tạo role mới
- [`edit_role`]() — Chỉnh sửa role (tên, màu, permission, hoist, mentionable)
- [`delete_role`]() — Xóa role
- [`assign_role`]() — Gán role cho user
- [`remove_role`]() — Gỡ role khỏi user

---

#### Moderation
- [`kick_member`]() — Kick member khỏi server
- [`ban_member`]() — Ban user khỏi server
- [`unban_member`]() — Gỡ ban user
- [`timeout_member`]() — Timeout member trong thời gian nhất định
- [`remove_timeout`]() — Gỡ timeout sớm
- [`set_nickname`]() — Đổi nickname của member
- [`get_bans`]() — Lấy danh sách user bị ban và lý do

---

#### Voice & Stage Channel Management
- [`create_voice_channel`]() — Tạo voice channel mới
- [`create_stage_channel`]() — Tạo stage channel mới
- [`edit_voice_channel`]() — Chỉnh sửa voice/stage channel (tên, bitrate, user limit, region)
- [`move_member`]() — Di chuyển member sang voice channel khác
- [`disconnect_member`]() — Ngắt kết nối member khỏi voice channel
- [`modify_voice_state`]() — Server mute hoặc deafen member trong voice channel

---

#### Scheduled Events
- [`create_guild_scheduled_event`]() — Lên lịch event mới (voice, stage, hoặc external)
- [`edit_guild_scheduled_event`]() — Chỉnh sửa event hoặc thay đổi trạng thái
- [`delete_guild_scheduled_event`]() — Xóa scheduled event
- [`list_guild_scheduled_events`]() — Liệt kê tất cả event đang active/scheduled
- [`get_guild_scheduled_event_users`]() — Lấy danh sách user quan tâm tới event

---

#### Forum Management
- [`create_forum_channel`]() — Tạo forum channel mới
- [`edit_forum_channel`]() — Chỉnh sửa cấu hình forum channel
- [`list_forum_channels`]() — Liệt kê tất cả forum channel trong server
- [`get_forum_channel_info`]() — Lấy thông tin chi tiết về forum channel (bao gồm tags)
- [`list_forum_tags`]() — Liệt kê tất cả tag trong forum channel
- [`create_forum_post`]() — Tạo bài đăng (thread) mới trong forum channel
- [`list_forum_posts`]() — Liệt kê các bài đăng active trong forum channel
- [`modify_forum_post`]() — Chỉnh sửa trạng thái bài đăng (lock/unlock, archive, pin, tags)

---

#### Thread Management ✨ Mới
- [`create_thread`]() — Tạo public thread từ tin nhắn hoặc tạo mới trong channel
- [`create_private_thread`]() — Tạo private thread (chỉ member được mời mới thấy)
- [`get_thread`]() — Lấy thông tin chi tiết về thread
- [`list_threads`]() — Liệt kê tất cả thread trong một channel
- [`list_guild_threads`]() — Liệt kê tất cả thread đang active trong toàn bộ server
- [`archive_thread`]() — Archive hoặc unarchive thread
- [`lock_thread`]() — Lock hoặc unlock thread (ngăn gửi tin nhắn mới)
- [`add_thread_member`]() — Thêm member vào thread
- [`remove_thread_member`]() — Xóa member khỏi thread

---

#### Ticket System ✨ Mới
- [`create_ticket`]() — Tạo ticket hỗ trợ (tạo private channel riêng cho user và staff)
- [`close_ticket`]() — Đóng ticket và archive channel
- [`list_tickets`]() — Liệt kê tất cả ticket đang mở trong server

---

#### Poll Management ✨ Mới
- [`create_poll`]() — Tạo native Discord poll (dùng Discord Poll UI chính thức, hỗ trợ tối đa 10 lựa chọn)
- [`get_poll_results`]() — Lấy kết quả và số lượt vote của poll
- [`end_poll`]() — Kết thúc poll sớm

---

#### Emoji Management
- [`list_emojis`]() — Liệt kê tất cả custom emoji trong server
- [`get_emoji_details`]() — Lấy thông tin chi tiết về emoji cụ thể
- [`create_emoji`]() — Tải lên custom emoji mới (base64 hoặc URL ảnh, tối đa 256KB)
- [`edit_emoji`]() — Chỉnh sửa tên hoặc role restriction của emoji
- [`delete_emoji`]() — Xóa custom emoji

---

#### Invite Management
- [`create_invite`]() — Tạo invite link mới cho channel
- [`list_invites`]() — Liệt kê tất cả invite đang active trong server
- [`delete_invite`]() — Xóa (thu hồi) invite link
- [`get_invite_details`]() — Lấy thông tin về một invite link (public invite)

---

<hr>

## 📚 Tài liệu bổ sung

- [INTERACTION_TOOLS_DOCS.md](./INTERACTION_TOOLS_DOCS.md) — Hướng dẫn chi tiết về Interaction tools
- [COMPONENT_HANDLER_GUIDE.md](./COMPONENT_HANDLER_GUIDE.md) — Hướng dẫn xử lý Component interactions
- [TOOL_SCHEMA.md](./TOOL_SCHEMA.md) — Schema chi tiết của từng tool

## 🙏 Credits

- Fork từ [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp)
- Discord API: [JDA (Java Discord API)](https://jda.wiki/)
- MCP Framework: [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/)

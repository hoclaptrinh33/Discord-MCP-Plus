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
    <a href="https://github.com/SaseQ/discord-mcp" target="_blank" style="margin: 2px;">
        <img alt="Forked from SaseQ/discord-mcp" src="https://img.shields.io/badge/forked%20from-SaseQ%2Fdiscord--mcp-blue?logo=github" style="display: inline-block; vertical-align: middle;"/>
    </a>
</div>

<div align="center">
  <h3>
    <a href="README.md">English</a> | <span>Tiếng Việt</span>
  </h3>
</div>

---

> [!NOTE]
> 📌 **Nguồn gốc dự án:** [**Discord-MCP-Plus**](https://github.com/hoclaptrinh33/Discord-MCP-Plus) là phiên bản được fork và phát triển nâng cấp từ bản gốc [**SaseQ/discord-mcp**](https://github.com/SaseQ/discord-mcp).

# 🇻🇳 Discord MCP Plus (Tiếng Việt)

## 📖 Mô tả

**Discord MCP Plus** là một [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) server mở rộng cho Discord API, được xây dựng trên nền tảng [Java Discord API (JDA)](https://jda.wiki/).

Được phát triển dưới dạng bản fork nâng cấp chuyên sâu từ bản gốc [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp), dự án này bổ sung rất nhiều tính năng mạnh mẽ:
- 🧵 Quản lý **Thread** (luồng thảo luận) nâng cao
- 🎫 Hệ thống **Ticket** hỗ trợ người dùng tự động
- 📊 Tạo cuộc bình chọn (**Poll**) chuẩn native Discord
- 🎭 Hệ thống **Modal** và **Interaction** toàn diện
- 🔐 Kiểm tra **Permission** (quyền hạn) và truy xuất **Audit Log**
- ✅ Tự động đăng ký các **Slash Command**

## ✨ Tính năng bổ sung so với bản gốc

| Nhóm tính năng | Danh sách công cụ (Tools) mới |
|---------------|-------------------------------|
| **Tương tác (Interactions)** | `list_pending_interactions`, `get_interaction`, `respond_interaction`, `defer_interaction`, `edit_interaction_response`, `followup_interaction`, `delete_interaction_response`, `respond_autocomplete`, `respond_with_modal` |
| **Lệnh chém (Slash Commands)** | `register_slash_command` |
| **Quyền hạn (Permissions)** | `check_user_permission`, `check_bot_permission`, `get_effective_permissions`, `get_audit_logs`, `sync_channel_permissions_with_category`, `sync_all_channels_in_category` |
| **Bình chọn (Polls)** | `create_poll`, `get_poll_results`, `end_poll` |
| **Luồng (Threads)** | `create_thread`, `create_private_thread`, `get_thread`, `list_threads`, `list_guild_threads`, `archive_thread`, `lock_thread`, `add_thread_member`, `remove_thread_member` |
| **Vé hỗ trợ (Tickets)** | `create_ticket`, `close_ticket`, `list_tickets` |
| **Hộp thoại (Modals)** | `create_modal_payload`, `send_modal`, `respond_modal` |
| **Tệp đính kèm (Attachments)** | `get_attachment` |
| **Webhooks** | `edit_webhook` (bổ sung vào bộ công cụ webhook) |

## 🔬 Cài đặt

### ► 🐳 Docker (Khuyến nghị)

> [!NOTE]
> Yêu cầu Docker. Tham khảo hướng dẫn cài đặt tại [docker.com](https://www.docker.com/products/docker-desktop/).

#### 1) Thiết lập biến môi trường
```bash
export DISCORD_TOKEN="YOUR_DISCORD_BOT_TOKEN"
export DISCORD_GUILD_ID="OPTIONAL_DEFAULT_SERVER_ID"
export SPRING_PROFILES_ACTIVE=http
```

> [!IMPORTANT]
> Hướng dẫn tạo Discord bot và lấy token: [discordjs.guide](https://discordjs.guide/legacy/preparations/app-setup)

> [!TIP]
> `DISCORD_GUILD_ID` là tùy chọn. Khi được cấu hình sẵn, các tool có tham số `guildId` có thể bỏ qua tham số này.

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
cd Discord-MCP-Plus
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

#### 4) Kiểm tra hoạt động
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

#### 2) Build dự án
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

## 🔗 Kết nối với Client

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

Vào menu: `Settings` → `Cursor Settings` → `MCP` → `Add new global MCP server`

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

1. Mở giao diện n8n, thêm node **MCP Client**.
2. Chọn giao thức truyền tải **HTTP** hoặc **Streamable HTTP**.
3. Điền URL: `http://localhost:8085/mcp`
4. Lưu cấu hình và kiểm tra kết nối.

> Nếu n8n chạy trong môi trường Docker, hãy dùng tên service: `http://discord-mcp-plus:8085/mcp`

</details>

## 🛠️ Danh sách Tool

> Nếu biến môi trường `DISCORD_GUILD_ID` đã được cấu hình, tham số `guildId` là không bắt buộc đối với tất cả các tool.

---

#### Thông tin Server
- [`get_server_info`]() — Lấy thông tin chi tiết về Discord server

---

#### Quản lý Người dùng
- [`get_user_id_by_name`]() — Lấy User ID theo username trong server (tiện lợi khi cần ping `<@id>`)
- [`send_private_message`]() — Gửi tin nhắn trực tiếp (DM) tới người dùng
- [`edit_private_message`]() — Chỉnh sửa tin nhắn trực tiếp đã gửi
- [`delete_private_message`]() — Xóa tin nhắn trực tiếp
- [`read_private_messages`]() — Đọc lịch sử tin nhắn trực tiếp (hỗ trợ `count` 1-100, con trỏ định vị `before`/`after`/`around`)

---

#### Quản lý Tin nhắn
- [`send_message`]() — Gửi tin nhắn vào kênh (hỗ trợ embeds, components tương tác, tệp đính kèm)
- [`edit_message`]() — Chỉnh sửa nội dung tin nhắn trong kênh
- [`delete_message`]() — Xóa tin nhắn trong kênh
- [`read_messages`]() — Đọc lịch sử tin nhắn trong kênh (hỗ trợ `count` 1-100, con trỏ `before`/`after`/`around`)
- [`add_reaction`]() — Thả biểu cảm cảm xúc (emoji reaction) vào tin nhắn
- [`remove_reaction`]() — Gỡ bỏ biểu cảm khỏi tin nhắn
- [`get_attachment`]() — Lấy thông tin chi tiết và URL tải xuống của tệp đính kèm trong tin nhắn

---

#### Quản lý Kênh (Channel)
- [`create_text_channel`]() — Tạo kênh văn bản (text channel) mới
- [`edit_text_channel`]() — Cập nhật cấu hình kênh văn bản (tên, chủ đề topic, nsfw, chế độ chậm slowmode, danh mục cha, thứ tự hiển thị)
- [`delete_channel`]() — Xóa một kênh
- [`find_channel`]() — Tìm kiếm kênh theo tên và guild ID
- [`list_channels`]() — Liệt kê tất cả các kênh có trong server
- [`get_channel_info`]() — Lấy thông tin chi tiết về một kênh
- [`move_channel`]() — Di chuyển kênh sang category khác hoặc điều chỉnh vị trí thứ tự

---

#### Quản lý Danh mục (Category)
- [`create_category`]() — Tạo danh mục (category) mới
- [`edit_category`]() — Đổi tên hoặc sắp xếp lại vị trí danh mục
- [`delete_category`]() — Xóa danh mục
- [`find_category`]() — Tìm danh mục theo tên
- [`list_channels_in_category`]() — Liệt kê các kênh thuộc một danh mục cụ thể

---

#### Quyền hạn Kênh (Channel Permissions)
- [`list_channel_permission_overwrites`]() — Liệt kê tất cả các quyền ghi đè (permission overwrites) của kênh
- [`upsert_role_channel_permissions`]() — Tạo hoặc cập nhật quyền ghi đè trên kênh cho một vai trò (role)
- [`upsert_member_channel_permissions`]() — Tạo hoặc cập nhật quyền ghi đè trên kênh cho một thành viên
- [`delete_channel_permission_overwrite`]() — Xóa quyền ghi đè của vai trò hoặc thành viên trên kênh
- [`get_effective_permissions`]() — Lấy quyền hiệu dụng thực tế của người dùng/vai trò trên một kênh cụ thể
- [`sync_channel_permissions_with_category`]() — Đồng bộ quyền của kênh theo danh mục cha (parent category)
- [`sync_all_channels_in_category`]() — Đồng bộ quyền của toàn bộ các kênh bên trong danh mục cha

---

#### Quyền hạn & Nhật ký (Permission & Audit)
- [`check_user_permission`]() — Kiểm tra xem người dùng có quyền cụ thể trong kênh hoặc trên toàn server hay không
- [`check_bot_permission`]() — Kiểm tra xem bot có quyền cụ thể trong kênh hoặc trên toàn server hay không
- [`get_audit_logs`]() — Truy xuất nhật ký hoạt động (audit logs) của server (hỗ trợ lọc theo loại hành động, người thực hiện, giới hạn số lượng)

---

#### Quản lý Lệnh Slash Command
- [`register_slash_command`]() — Đăng ký lệnh slash command mới cho server

---

#### Quản lý Tương tác (Interactions)
- [`list_pending_interactions`]() — Liệt kê các tương tác đang chờ được xử lý
- [`get_interaction`]() — Lấy thông tin chi tiết về một tương tác dựa trên token
- [`respond_interaction`]() — Phản hồi tương tác bằng tin nhắn (phản hồi khởi đầu)
- [`defer_interaction`]() — Trì hoãn phản hồi tương tác (gửi xác nhận không nội dung, áp dụng cho tác vụ xử lý kéo dài)
- [`edit_interaction_response`]() — Chỉnh sửa nội dung tin nhắn phản hồi khởi đầu
- [`followup_interaction`]() — Gửi tin nhắn tiếp nối (follow-up) sau khi đã trì hoãn
- [`delete_interaction_response`]() — Xóa tin nhắn phản hồi của tương tác
- [`respond_autocomplete`]() — Phản hồi tương tác autocomplete với danh sách tùy chọn gợi ý
- [`respond_with_modal`]() — Phản hồi tương tác nút bấm/menu bằng cách hiển thị một modal (hộp thoại nhập liệu)

---

#### Quản lý Hộp thoại biểu mẫu (Modals)
- [`create_modal_payload`]() — Tạo cấu trúc dữ liệu cho modal (tiêu đề, các trường văn bản) để sử dụng với tương tác
- [`send_modal`]() — Gửi hiển thị modal tới người dùng thông qua interaction token
- [`respond_modal`]() — Tiếp nhận và xử lý dữ liệu gửi về khi người dùng submit modal

---

#### Quản lý Webhook
- [`create_webhook`]() — Tạo webhook mới trên một kênh
- [`delete_webhook`]() — Xóa webhook
- [`list_webhooks`]() — Liệt kê toàn bộ webhook được thiết lập trên kênh
- [`send_webhook_message`]() — Đăng tải tin nhắn thông qua webhook
- [`edit_webhook`]() — Cập nhật cấu hình webhook (tên hiển thị, avatar đại diện, kênh liên kết)

---

#### Quản lý Vai trò (Roles)
- [`list_roles`]() — Liệt kê tất cả các vai trò (roles) trong server
- [`create_role`]() — Tạo vai trò mới
- [`edit_role`]() — Chỉnh sửa thuộc tính vai trò (tên gọi, màu sắc, quyền hạn, hiển thị riêng, cho phép nhắc đến)
- [`delete_role`]() — Xóa vai trò
- [`assign_role`]() — Gán vai trò cho một thành viên
- [`remove_role`]() — Gỡ bỏ vai trò khỏi một thành viên

---

#### Quản trị & Điều phối (Moderation)
- [`kick_member`]() — Đuổi (kick) thành viên ra khỏi server
- [`ban_member`]() — Cấm (ban) thành viên vĩnh viễn khỏi server
- [`unban_member`]() — Gỡ lệnh cấm (unban) cho người dùng
- [`timeout_member`]() — Đặt khoảng thời gian chờ (timeout) đối với thành viên
- [`remove_timeout`]() — Gỡ bỏ hạn chế timeout sớm cho thành viên
- [`set_nickname`]() — Thay đổi biệt danh (nickname) của thành viên
- [`get_bans`]() — Xem danh sách người dùng đang bị cấm và lý do tương ứng

---

#### Kênh Thoại & Sân khấu (Voice & Stage)
- [`create_voice_channel`]() — Tạo kênh đàm thoại (voice channel) mới
- [`create_stage_channel`]() — Tạo kênh sân khấu (stage channel) mới
- [`edit_voice_channel`]() — Cập nhật kênh thoại/sân khấu (tên, bitrate, giới hạn người tham gia, khu vực máy chủ)
- [`move_member`]() — Chuyển thành viên sang kênh thoại khác
- [`disconnect_member`]() — Ngắt kết nối thành viên khỏi kênh thoại
- [`modify_voice_state`]() — Tắt tiếng (mute) hoặc tắt âm (deafen) cấp máy chủ đối với thành viên trong kênh thoại

---

#### Quản lý Sự kiện (Scheduled Events)
- [`create_guild_scheduled_event`]() — Lên lịch tạo sự kiện mới (trên kênh thoại, kênh sân khấu hoặc địa điểm bên ngoài)
- [`edit_guild_scheduled_event`]() — Chỉnh sửa thông tin sự kiện hoặc cập nhật trạng thái diễn ra
- [`delete_guild_scheduled_event`]() — Hủy bỏ sự kiện đã lên lịch
- [`list_guild_scheduled_events`]() — Liệt kê tất cả các sự kiện đang diễn ra hoặc đã lên lịch
- [`get_guild_scheduled_event_users`]() — Xem danh sách những người dùng đăng ký tham gia sự kiện

---

#### Quản lý Diễn đàn (Forum)
- [`create_forum_channel`]() — Tạo kênh diễn đàn (forum channel) mới
- [`edit_forum_channel`]() — Cập nhật cấu hình kênh diễn đàn
- [`list_forum_channels`]() — Liệt kê tất cả các kênh diễn đàn có trong server
- [`get_forum_channel_info`]() — Lấy thông tin chi tiết về kênh diễn đàn (bao gồm danh sách thẻ tags)
- [`list_forum_tags`]() — Liệt kê tất cả các thẻ tag có trong kênh diễn đàn
- [`create_forum_post`]() — Đăng bài viết mới (kèm luồng thảo luận riêng) trong kênh diễn đàn
- [`list_forum_posts`]() — Liệt kê các bài viết đang hoạt động trong kênh diễn đàn
- [`modify_forum_post`]() — Điều chỉnh trạng thái bài viết (khóa/mở khóa, lưu trữ, ghim nổi bật, gắn thẻ tags)

---

#### Quản lý Luồng thảo luận (Thread) ✨ Mới
- [`create_thread`]() — Tạo thread công khai từ một tin nhắn hoặc tạo mới trực tiếp trong kênh
- [`create_private_thread`]() — Tạo thread riêng tư (chỉ những thành viên được mời mới nhìn thấy)
- [`get_thread`]() — Lấy thông tin chi tiết về thread
- [`list_threads`]() — Liệt kê tất cả các thread trong một kênh
- [`list_guild_threads`]() — Liệt kê các thread đang hoạt động trên toàn bộ server
- [`archive_thread`]() — Lưu trữ (archive) hoặc khôi phục lại thread
- [`lock_thread`]() — Khóa (lock) hoặc mở khóa thread (ngăn chặn gửi tin nhắn mới)
- [`add_thread_member`]() — Thêm một thành viên vào thread
- [`remove_thread_member`]() — Gỡ bỏ thành viên khỏi thread

---

#### Hệ thống Ticket Hỗ trợ ✨ Mới
- [`create_ticket`]() — Khởi tạo ticket hỗ trợ (tạo kênh riêng tư giữa người dùng và ban quản trị/staff)
- [`close_ticket`]() — Đóng ticket và lưu trữ kênh hỗ trợ
- [`list_tickets`]() — Liệt kê tất cả các ticket đang mở trong server

---

#### Quản lý Bình chọn (Polls) ✨ Mới
- [`create_poll`]() — Tạo cuộc bình chọn native Discord (hiển thị UI Poll chính thức của Discord, hỗ trợ tối đa 10 phương án lựa chọn)
- [`get_poll_results`]() — Lấy kết quả hiện tại và số lượt bình chọn của poll
- [`end_poll`]() — Kết thúc cuộc bình chọn trước thời hạn

---

#### Quản lý Biểu tượng cảm xúc (Emoji)
- [`list_emojis`]() — Liệt kê toàn bộ emoji tùy chỉnh trong server
- [`get_emoji_details`]() — Lấy thông tin chi tiết về một emoji cụ thể
- [`create_emoji`]() — Tải lên emoji tùy chỉnh mới (hỗ trợ chuỗi base64 hoặc URL ảnh, kích thước tối đa 256KB)
- [`edit_emoji`]() — Đổi tên hoặc phân quyền vai trò được phép dùng emoji
- [`delete_emoji`]() — Xóa emoji tùy chỉnh khỏi server

---

#### Quản lý Lời mời (Invite)
- [`create_invite`]() — Tạo đường dẫn mời mới cho kênh
- [`list_invites`]() — Liệt kê các liên kết mời đang còn hiệu lực trong server
- [`delete_invite`]() — Xóa (thu hồi) liên kết mời
- [`get_invite_details`]() — Xem thông tin chi tiết về một liên kết mời công khai

---

## 📚 Tài liệu bổ sung

- [INTERACTION_TOOLS_DOCS.md](./INTERACTION_TOOLS_DOCS.md) — Hướng dẫn chi tiết về các công cụ Interaction
- [COMPONENT_HANDLER_GUIDE.md](./COMPONENT_HANDLER_GUIDE.md) — Hướng dẫn xử lý Component interactions
- [TOOL_SCHEMA.md](./TOOL_SCHEMA.md) — Schema chi tiết của từng tool

## 🙏 Đóng góp & Nguồn gốc (Credits)

- **Fork và nâng cấp từ**: [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp)
- **Kho lưu trữ (Repository)**: [hoclaptrinh33/Discord-MCP-Plus](https://github.com/hoclaptrinh33/Discord-MCP-Plus)
- **Discord API**: [JDA (Java Discord API)](https://jda.wiki/)
- **MCP Framework**: [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/)

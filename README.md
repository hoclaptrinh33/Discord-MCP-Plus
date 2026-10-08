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
    <span>English</span> | <a href="README.vi.md">Tiếng Việt</a>
  </h3>
</div>

---

> [!NOTE]
> 📌 **Project Origin:** [**Discord-MCP-Plus**](https://github.com/hoclaptrinh33/Discord-MCP-Plus) is forked and significantly upgraded from the original [**SaseQ/discord-mcp**](https://github.com/SaseQ/discord-mcp).

# 🇺🇸 Discord MCP Plus

## 📖 Description

**Discord MCP Plus** is a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) server extension for the Discord API, built on top of the [Java Discord API (JDA)](https://jda.wiki/).

Developed as an advanced fork of the original [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp), this project introduces extensive improvements and new capabilities:
- 🧵 Advanced **Thread** management
- 🎫 Automated support **Ticket** system
- 📊 Native Discord **Polls**
- 🎭 Full **Modal** and **Interaction** system support
- 🔐 Detailed **Permission** and **Audit Log** checks
- ✅ Automatic registration of **Slash Commands**

## ✨ Added Features Compared to the Original

| Group | New Tools |
|------|-----------|
| **Interactions** | `list_pending_interactions`, `get_interaction`, `respond_interaction`, `defer_interaction`, `edit_interaction_response`, `followup_interaction`, `delete_interaction_response`, `respond_autocomplete`, `respond_with_modal` |
| **Slash Commands** | `register_slash_command` |
| **Permissions** | `check_user_permission`, `check_bot_permission`, `get_effective_permissions`, `get_audit_logs`, `sync_channel_permissions_with_category`, `sync_all_channels_in_category` |
| **Polls** | `create_poll`, `get_poll_results`, `end_poll` |
| **Threads** | `create_thread`, `create_private_thread`, `get_thread`, `list_threads`, `list_guild_threads`, `archive_thread`, `lock_thread`, `add_thread_member`, `remove_thread_member` |
| **Tickets** | `create_ticket`, `close_ticket`, `list_tickets` |
| **Modals** | `create_modal_payload`, `send_modal`, `respond_modal` |
| **Attachments** | `get_attachment` |
| **Webhooks** | `edit_webhook` (added to webhook suite) |

## 🔬 Installation

### ► 🐳 Docker (Recommended)

> [!NOTE]
> Requires Docker. Installation guide at [docker.com](https://www.docker.com/products/docker-desktop/).

#### 1) Set Environment Variables
```bash
export DISCORD_TOKEN="YOUR_DISCORD_BOT_TOKEN"
export DISCORD_GUILD_ID="OPTIONAL_DEFAULT_SERVER_ID"
export SPRING_PROFILES_ACTIVE=http
```

> [!IMPORTANT]
> Guide to create a Discord bot and get its token: [discordjs.guide](https://discordjs.guide/legacy/preparations/app-setup)

> [!TIP]
> `DISCORD_GUILD_ID` is optional. When provided, tools with a `guildId` parameter can omit that parameter.

#### 2) Run the Container
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

#### 1) Clone the Repository
```bash
git clone https://github.com/hoclaptrinh33/Discord-MCP-Plus
cd Discord-MCP-Plus
```

#### 2) Create `.env` file
```bash
cat > .env <<EOF
SPRING_PROFILES_ACTIVE=http
DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN>
DISCORD_GUILD_ID=<OPTIONAL_DEFAULT_SERVER_ID>
EOF
```

#### 3) Start Container
```bash
docker compose up -d --build
```

#### 4) Verify
```bash
docker ps --filter name=discord-mcp-plus
curl -fsS http://localhost:8085/actuator/health
```

MCP endpoint: `http://localhost:8085/mcp`

Health endpoint: `http://localhost:8085/actuator/health`

</details>

<details>
    <summary style="font-size: 1.35em; font-weight: bold;">
        🔧 Manual Installation (Maven)
    </summary>

#### 1) Clone the Repository
```bash
git clone https://github.com/hoclaptrinh33/Discord-MCP-Plus
cd Discord-MCP-Plus
```

#### 2) Build Project
```bash
mvn clean package
```

#### 3) Run JAR
```bash
DISCORD_TOKEN=<YOUR_DISCORD_BOT_TOKEN> \
DISCORD_GUILD_ID=<OPTIONAL_DEFAULT_SERVER_ID> \
SPRING_PROFILES_ACTIVE=http \
java -jar target/discord-mcp-*.jar
```

MCP endpoint: `http://localhost:8085/mcp`

</details>

## 🔗 Connection

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

Go to: `Settings` → `Cursor Settings` → `MCP` → `Add new global MCP server`

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

1. Open n8n, add **MCP Client** node.
2. Choose transport **HTTP** or **Streamable HTTP**.
3. URL: `http://localhost:8085/mcp`
4. Save and test connection.

> If n8n runs inside Docker, use service name: `http://discord-mcp-plus:8085/mcp`

</details>

## 🛠️ Tool List

> If `DISCORD_GUILD_ID` is configured, the `guildId` parameter is optional for all tools.

---

#### Server Information
- [`get_server_info`]() — Retrieves detailed information about the Discord server

---

#### User Management
- [`get_user_id_by_name`]() — Get User ID by username in the server (useful to ping `<@id>`)
- [`send_private_message`]() — Send a direct message (DM) to a user
- [`edit_private_message`]() — Edit a sent private message
- [`delete_private_message`]() — Delete a private message
- [`read_private_messages`]() — Read DM history (supports `count` 1-100, cursors `before`/`after`/`around`)

---

#### Message Management
- [`send_message`]() — Send a message to a channel (supports embeds, components, files)
- [`edit_message`]() — Edit a message in a channel
- [`delete_message`]() — Delete a message
- [`read_messages`]() — Read message history (supports `count` 1-100, cursors `before`/`after`/`around`)
- [`add_reaction`]() — Add a reaction (emoji) to a message
- [`remove_reaction`]() — Remove a reaction from a message
- [`get_attachment`]() — Get attachment information and download URL from a message

---

#### Channel Management
- [`create_text_channel`]() — Create a new text channel
- [`edit_text_channel`]() — Edit text channel configuration (name, topic, nsfw, slowmode, category, position)
- [`delete_channel`]() — Delete a channel
- [`find_channel`]() — Find channel by name and guild ID
- [`list_channels`]() — List all channels in the server
- [`get_channel_info`]() — Get detailed channel information
- [`move_channel`]() — Move channel to another category or change its position

---

#### Category Management
- [`create_category`]() — Create a new category
- [`edit_category`]() — Rename or change category position
- [`delete_category`]() — Delete a category
- [`find_category`]() — Find category by name
- [`list_channels_in_category`]() — List all channels in a specific category

---

#### Channel Permissions
- [`list_channel_permission_overwrites`]() — List all permission overwrites for a channel
- [`upsert_role_channel_permissions`]() — Create/update channel permission overwrites for a role
- [`upsert_member_channel_permissions`]() — Create/update channel permission overwrites for a member
- [`delete_channel_permission_overwrite`]() — Delete channel permission overwrites for a role/member
- [`get_effective_permissions`]() — Get effective permissions of a user/role on a specific channel
- [`sync_channel_permissions_with_category`]() — Sync channel permissions with its parent category
- [`sync_all_channels_in_category`]() — Sync permissions of all channels within a category

---

#### Permission & Audit
- [`check_user_permission`]() — Check if a user has a specific permission in a channel/server
- [`check_bot_permission`]() — Check if the bot has a specific permission in a channel/server
- [`get_audit_logs`]() — Get guild audit logs (filterable by action type, user, limit)

---

#### Slash Command Management
- [`register_slash_command`]() — Register a new slash command in the server

---

#### Interaction Management
- [`list_pending_interactions`]() — List all pending interactions waiting for processing
- [`get_interaction`]() — Get details of a specific interaction by token
- [`respond_interaction`]() — Respond to an interaction with a message (initial response)
- [`defer_interaction`]() — Defer the interaction response (acknowledge without content, for long operations)
- [`edit_interaction_response`]() — Edit the initial response message of an interaction
- [`followup_interaction`]() — Send a follow-up message after deferring
- [`delete_interaction_response`]() — Delete the response message of an interaction
- [`respond_autocomplete`]() — Respond to an autocomplete interaction with suggested choices
- [`respond_with_modal`]() — Respond to a component interaction by opening a modal

---

#### Modal Management
- [`create_modal_payload`]() — Create modal payload (title, fields) to be used with an interaction
- [`send_modal`]() — Send a modal to a user using an interaction token
- [`respond_modal`]() — Process response from modal submission

---

#### Webhook Management
- [`create_webhook`]() — Create a new webhook on a channel
- [`delete_webhook`]() — Delete a webhook
- [`list_webhooks`]() — List all webhooks on a channel
- [`send_webhook_message`]() — Send a message via webhook
- [`edit_webhook`]() — Edit webhook configuration (name, avatar, channel)

---

#### Role Management
- [`list_roles`]() — List all roles in the server
- [`create_role`]() — Create a new role
- [`edit_role`]() — Edit role (name, color, permission, hoist, mentionable)
- [`delete_role`]() — Delete a role
- [`assign_role`]() — Assign a role to a user
- [`remove_role`]() — Remove a role from a user

---

#### Moderation
- [`kick_member`]() — Kick a member from the server
- [`ban_member`]() — Ban a user from the server
- [`unban_member`]() — Unban a user
- [`timeout_member`]() — Timeout a member for a specified duration
- [`remove_timeout`]() — Remove timeout early
- [`set_nickname`]() — Change nickname of a member
- [`get_bans`]() — Get list of banned users and reasons

---

#### Voice & Stage Channel Management
- [`create_voice_channel`]() — Create a new voice channel
- [`create_stage_channel`]() — Create a new stage channel
- [`edit_voice_channel`]() — Edit voice/stage channel (name, bitrate, user limit, region)
- [`move_member`]() — Move a member to another voice channel
- [`disconnect_member`]() — Disconnect a member from a voice channel
- [`modify_voice_state`]() — Server mute or deafen a member in a voice channel

---

#### Scheduled Events
- [`create_guild_scheduled_event`]() — Schedule a new event (voice, stage, or external)
- [`edit_guild_scheduled_event`]() — Edit an event or change its status
- [`delete_guild_scheduled_event`]() — Delete a scheduled event
- [`list_guild_scheduled_events`]() — List all active/scheduled events
- [`get_guild_scheduled_event_users`]() — Get list of users interested in an event

---

#### Forum Management
- [`create_forum_channel`]() — Create a new forum channel
- [`edit_forum_channel`]() — Edit forum channel configuration
- [`list_forum_channels`]() — List all forum channels in the server
- [`get_forum_channel_info`]() — Get detailed forum channel info (including tags)
- [`list_forum_tags`]() — List all tags in a forum channel
- [`create_forum_post`]() — Create a new post (thread) in a forum channel
- [`list_forum_posts`]() — List active posts in a forum channel
- [`modify_forum_post`]() — Modify post status (lock/unlock, archive, pin, tags)

---

#### Thread Management ✨ New
- [`create_thread`]() — Create a public thread from a message or create new in channel
- [`create_private_thread`]() — Create a private thread (only invited members can see)
- [`get_thread`]() — Get detailed thread info
- [`list_threads`]() — List all threads in a channel
- [`list_guild_threads`]() — List all active threads in the entire server
- [`archive_thread`]() — Archive or unarchive a thread
- [`lock_thread`]() — Lock or unlock a thread (prevents sending new messages)
- [`add_thread_member`]() — Add a member to a thread
- [`remove_thread_member`]() — Remove a member from a thread

---

#### Ticket System ✨ New
- [`create_ticket`]() — Create support ticket (creates a private channel for user & staff)
- [`close_ticket`]() — Close ticket and archive channel
- [`list_tickets`]() — List all open tickets in the server

---

#### Poll Management ✨ New
- [`create_poll`]() — Create a native Discord poll (uses official Discord Poll UI, up to 10 choices)
- [`get_poll_results`]() — Get poll results and vote counts
- [`end_poll`]() — End a poll early

---

#### Emoji Management
- [`list_emojis`]() — List all custom emojis in the server
- [`get_emoji_details`]() — Get detailed info about a specific emoji
- [`create_emoji`]() — Upload a new custom emoji (base64 or image URL, max 256KB)
- [`edit_emoji`]() — Edit emoji name or role restrictions
- [`delete_emoji`]() — Delete custom emoji

---

#### Invite Management
- [`create_invite`]() — Create a new invite link for a channel
- [`list_invites`]() — List all active invites in the server
- [`delete_invite`]() — Delete (revoke) an invite link
- [`get_invite_details`]() — Get information about an invite link (public invite)

---

## 📚 Additional Documentation

- [INTERACTION_TOOLS_DOCS.md](./INTERACTION_TOOLS_DOCS.md) — Detailed guide on Interaction tools
- [COMPONENT_HANDLER_GUIDE.md](./COMPONENT_HANDLER_GUIDE.md) — Guide to handle Component interactions
- [TOOL_SCHEMA.md](./TOOL_SCHEMA.md) — Detailed schema of each tool

## 🙏 Credits

- **Forked & upgraded from**: [SaseQ/discord-mcp](https://github.com/SaseQ/discord-mcp)
- **Repository**: [hoclaptrinh33/Discord-MCP-Plus](https://github.com/hoclaptrinh33/Discord-MCP-Plus)
- **Discord API**: [JDA (Java Discord API)](https://jda.wiki/)
- **MCP Framework**: [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/)

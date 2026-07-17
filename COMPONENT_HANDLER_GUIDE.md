# Discord MCP Fork - Component Handler Guide

## Tổng quan

Discord MCP Fork hỗ trợ **2 cơ chế xử lý button/select menu**:

1. **Hardcode Convention** - Pattern `custom_id` cố định, bot tự xử lý ngay
2. **Dynamic Registry** - Đăng ký handler động qua `handlers.json`, linh hoạt cao

Cả 2 cơ chế đều **KHÔNG CẦN Hermes** để xử lý interaction. Chỉ cần Discord bot là đủ.

---

## 1. HARDCODE CONVENTION (Pattern cố định)

Sử dụng khi bạn muốn **nhanh, đơn giản, không cần đăng ký**.

### Pattern hỗ trợ:

| Pattern | Mô tả | Ví dụ custom_id | Kết quả |
|---------|-------|-----------------|---------|
| `role_add:<role_id>` | Thêm role cho user | `role_add:1522859794543939657` | ✅ Đã thêm role: XXX |
| `role_remove:<role_id>` | Xóa role | `role_remove:1522859794543939657` | ✅ Đã xóa role: XXX |
| `role_toggle:<role_id>` | Toggle role (có ↔ không) | `role_toggle:1522859794543939657` | ✅ Đã thêm/xóa role |
| `vote:<poll_id>:<option>` | Bỏ phiếu | `vote:poll_001:nemotron` | 🗳️ Đã ghi nhận vote |
| `ticket_create:<type>` | Tạo ticket | `ticket_create:support` | 🎫 Ticket system... |
| `ticket_close` | Đóng ticket | `ticket_close` | ✅ Đã đóng ticket |
| `channel_lock` | Khóa channel | `channel_lock` | 🔒 Channel lock... |
| `channel_unlock` | Mở khóa channel | `channel_unlock` | 🔓 Channel unlock... |
| `message_delete` | Xóa message | `message_delete` | 🗑️ Đã xóa message |
| `react:<emoji>` | Tự động react | `react:✅` | (react emoji) |

### Ví dụ sử dụng:

```json
// Gửi message với button
{
  "message": "Nhận role Member",
  "componentsJson": [{
    "type": 1,
    "components": [{
      "type": 2,
      "style": 1,
      "label": "Nhận Member",
      "custom_id": "role_add:1522859794543939657",
      "emoji": {"name": "✅"}
    }]
  }]
}
```

Khi user bấm nút → Bot **tự động** thêm role, không cần Hermes.

---

## 2. DYNAMIC REGISTRY (handlers.json)

Sử dụng khi bạn muốn **linh hoạt, AI định nghĩa action động**.

### File: `handlers.json`

```json
{
  "btn_get_member": {
    "action": "ADD_ROLE",
    "params": {
      "role_id": "1522859794543939657"
    },
    "response": {
      "ephemeral": true,
      "content": "✅ Đã nhận role Member!"
    }
  },
  "btn_vote_nemotron": {
    "action": "VOTE",
    "params": {
      "poll_id": "poll_001",
      "option": "nemotron"
    },
    "response": {
      "ephemeral": true,
      "content": "🗳️ Cảm ơn bạn đã vote Nemotron!"
    }
  },
  "btn_create_support_ticket": {
    "action": "CREATE_TICKET",
    "params": {
      "type": "support",
      "category_id": "1522882869494677634"
    },
    "response": {
      "ephemeral": false,
      "content": "🎫 Ticket đã được tạo!"
    }
  }
}
```

### Cách hoạt động:

1. Bot load `handlers.json` khi khởi động
2. User bấm nút với `custom_id = "btn_get_member"`
3. Bot tra registry → tìm thấy handler `ADD_ROLE`
4. Bot thực thi → thêm role → gửi response
5. **KHÔNG CẦN Hermes**

### Đăng ký handler mới (runtime):

Gọi tool `register_handler` (nếu có) hoặc edit file `handlers.json` rồi restart bot.

---

## 3. FALLBACK: HERMES POLL

Nếu `custom_id` **KHÔNG** match convention VÀ **KHÔNG** có trong registry:

→ Bot deferReply → lưu interaction → Hermes poll qua `list_pending_interactions`

Dùng cho các button phức tạp cần AI xử lý.

---

## 4. ACTION TYPES HỖ TRỢ

### Registry actions:

| Action | Params | Mô tả |
|--------|--------|-------|
| `ADD_ROLE` | `role_id` | Thêm role |
| `REMOVE_ROLE` | `role_id` | Xóa role |
| `TOGGLE_ROLE` | `role_id` | Toggle role |
| `VOTE` | `poll_id`, `option` | Bỏ phiếu |
| `CREATE_TICKET` | `type`, `category_id` | Tạo ticket |
| `CLOSE_TICKET` | - | Đóng ticket |
| `LOCK_CHANNEL` | - | Khóa channel |
| `UNLOCK_CHANNEL` | - | Mở khóa |
| `DELETE_MESSAGE` | - | Xóa message |
| `REACT` | `emoji` | React emoji |

---

## 5. LƯU Ý QUAN TRỌNG

1. **custom_id phải là snake_case** (không phải camelCase)
2. Bot **tự động deferReply** để tránh timeout 3 giây
3. Convention patterns có **ưu tiên cao hơn** registry
4. `handlers.json` được **auto-save** khi đăng ký/unregister
5. Nếu Hermes crash → **button VẪN HOẠT ĐỘNG** (nếu dùng convention/registry)

---

## 6. VÍ DỤ THỰC TẾ

### Tạo nút nhận role (convention):

```json
{
  "custom_id": "role_add:1522859794543939657"
}
```

### Tạo nút vote (registry):

File `handlers.json`:
```json
{
  "vote_nemotron": {
    "action": "VOTE",
    "params": {"poll_id": "poll_ai", "option": "nemotron"},
    "response": {"ephemeral": true, "content": "✅ Vote thành công!"}
  }
}
```

Message:
```json
{
  "custom_id": "vote_nemotron"
}
```

---

**Tác giả:** Discord MCP Fork v1.0
**Ngày:** 2026-07-07
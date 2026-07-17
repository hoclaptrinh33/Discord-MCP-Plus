package dev.saseq.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages component handlers with JSON persistence.
 * Supports both hardcoded convention patterns and dynamic registry.
 */
public class ComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(ComponentHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String HANDLERS_FILE = "handlers.json";

    // Registry: custom_id -> HandlerConfig
    private final Map<String, HandlerConfig> registry = new ConcurrentHashMap<>();

    // Hardcoded convention patterns
    private static final String ROLE_ADD_PREFIX = "role_add:";
    private static final String ROLE_REMOVE_PREFIX = "role_remove:";
    private static final String ROLE_TOGGLE_PREFIX = "role_toggle:";
    // VOTE_PREFIX removed - we use native Discord Polls now (no custom vote: buttons)
    @Deprecated
    private static final String VOTE_PREFIX = "vote:";
    private static final String TICKET_CREATE_PREFIX = "ticket_create:";
    private static final String TICKET_CLOSE = "ticket_close";
    private static final String CHANNEL_LOCK = "channel_lock";
    private static final String CHANNEL_UNLOCK = "channel_unlock";
    private static final String MESSAGE_DELETE = "message_delete";
    private static final String REACT_PREFIX = "react:";

    private static final String OPEN_MODAL_PREFIX = "open_modal:";

    public ComponentHandler() {
        loadHandlers();
    }

    /**
     * Load handlers from handlers.json file
     */
    public void loadHandlers() {
        Path path = Paths.get(HANDLERS_FILE);
        if (!Files.exists(path)) {
            log.info("handlers.json not found, creating empty registry");
            saveHandlers();
            return;
        }

        try {
            String content = Files.readString(path);
            if (content.isBlank()) {
                log.info("handlers.json is empty");
                return;
            }

            JsonNode root = objectMapper.readTree(content);
            registry.clear();

            root.fields().forEachRemaining(entry -> {
                String customId = entry.getKey();
                JsonNode config = entry.getValue();
                HandlerConfig handler = parseHandlerConfig(config);
                if (handler != null) {
                    registry.put(customId, handler);
                    log.info("Loaded handler: {} -> {}", customId, handler.action);
                }
            });

            log.info("Loaded {} handlers from handlers.json", registry.size());
        } catch (IOException e) {
            log.error("Failed to load handlers.json: {}", e.getMessage());
        }
    }

    /**
     * Save registry to handlers.json
     */
    public void saveHandlers() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            for (Map.Entry<String, HandlerConfig> entry : registry.entrySet()) {
                ObjectNode config = objectMapper.valueToTree(entry.getValue());
                root.set(entry.getKey(), config);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(HANDLERS_FILE), root);
            log.info("Saved {} handlers to handlers.json", registry.size());
        } catch (IOException e) {
            log.error("Failed to save handlers.json: {}", e.getMessage());
        }
    }

    /**
     * Register a new handler dynamically
     */
    public boolean registerHandler(String customId, HandlerConfig config) {
        if (customId == null || config == null) {
            return false;
        }
        registry.put(customId, config);
        saveHandlers();
        log.info("Registered handler: {} -> {}", customId, config.action);
        return true;
    }

    /**
     * Unregister a handler
     */
    public boolean unregisterHandler(String customId) {
        if (registry.remove(customId) != null) {
            saveHandlers();
            log.info("Unregistered handler: {}", customId);
            return true;
        }
        return false;
    }

    /**
     * Get handler config by custom_id
     */
    public HandlerConfig getHandler(String customId) {
        return registry.get(customId);
    }

    /**
     * Check if custom_id matches any convention pattern
     */
    public boolean matchesConvention(String customId) {
        return customId != null && (
            customId.startsWith(ROLE_ADD_PREFIX) ||
            customId.startsWith(ROLE_REMOVE_PREFIX) ||
            customId.startsWith(ROLE_TOGGLE_PREFIX) ||
            // VOTE_PREFIX removed (native Discord Polls used instead)
            customId.startsWith(TICKET_CREATE_PREFIX) ||
            customId.equals(TICKET_CLOSE) ||
            customId.equals(CHANNEL_LOCK) ||
            customId.equals(CHANNEL_UNLOCK) ||
            customId.equals(MESSAGE_DELETE) ||
            customId.startsWith(REACT_PREFIX)
        );
    }

    /**
     * Execute hardcoded convention handler
     * Returns response message or null if handled
     */
    public String executeConvention(ButtonInteractionEvent event, String customId) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (guild == null || member == null) {
            return "❌ Lỗi: Không tìm thấy server hoặc thành viên";
        }

        // role_add:<role_id>
        if (customId.startsWith(ROLE_ADD_PREFIX)) {
            String roleId = customId.substring(ROLE_ADD_PREFIX.length());
            return addRole(guild, member, roleId);
        }

        // role_remove:<role_id>
        if (customId.startsWith(ROLE_REMOVE_PREFIX)) {
            String roleId = customId.substring(ROLE_REMOVE_PREFIX.length());
            return removeRole(guild, member, roleId);
        }

        // role_toggle:<role_id>
        if (customId.startsWith(ROLE_TOGGLE_PREFIX)) {
            String roleId = customId.substring(ROLE_TOGGLE_PREFIX.length());
            return toggleRole(guild, member, roleId);
        }

        // vote:* handling removed - native Discord Polls are used instead (no custom vote buttons)

        // ticket_create:<type>
        if (customId.startsWith(TICKET_CREATE_PREFIX)) {
            String type = customId.substring(TICKET_CREATE_PREFIX.length());
            return createTicket(guild, member, type, event.getChannel().getId());
        }

        // ticket_close
        if (customId.equals(TICKET_CLOSE)) {
            return closeTicket(event.getChannel().asTextChannel());
        }

        // channel_lock
        if (customId.equals(CHANNEL_LOCK)) {
            return lockChannel(event.getChannel().asTextChannel(), guild);
        }

        // channel_unlock
        if (customId.equals(CHANNEL_UNLOCK)) {
            return unlockChannel(event.getChannel().asTextChannel(), guild);
        }

        // message_delete
        if (customId.equals(MESSAGE_DELETE)) {
            event.getMessage().delete().queue();
            return "🗑️ Đã xóa message";
        }

        // react:<emoji>
        if (customId.startsWith(REACT_PREFIX)) {
            String emoji = customId.substring(REACT_PREFIX.length());
            event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emoji)).queue();
            return null; // No response needed
        }

        return "❌ Action không được hỗ trợ";
    }

    /**
     * Execute convention handler (overload - extract customId from event)
     */
    public String executeConvention(ButtonInteractionEvent event) {
        return executeConvention(event, event.getComponentId());
    }

    /**
     * Execute convention handler
     */
    public String executeRegistry(ButtonInteractionEvent event, HandlerConfig config) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (guild == null || member == null) {
            return "❌ Lỗi: Không tìm thấy server hoặc thành viên";
        }

        switch (config.action.toUpperCase()) {
            case "ADD_ROLE":
                String roleId = config.params.get("role_id");
                return addRole(guild, member, roleId);

            case "REMOVE_ROLE":
                roleId = config.params.get("role_id");
                return removeRole(guild, member, roleId);

            case "TOGGLE_ROLE":
                roleId = config.params.get("role_id");
                return toggleRole(guild, member, roleId);

            // VOTE case removed - use native Discord Polls

            case "CREATE_TICKET":
                String type = config.params.get("type");
                String categoryId = config.params.get("category_id");
                return createTicket(guild, member, type, categoryId);

            case "CLOSE_TICKET":
                return closeTicket(event.getChannel().asTextChannel());

            case "LOCK_CHANNEL":
                return lockChannel(event.getChannel().asTextChannel(), guild);

            case "UNLOCK_CHANNEL":
                return unlockChannel(event.getChannel().asTextChannel(), guild);

            case "DELETE_MESSAGE":
                event.getMessage().delete().queue();
                return "🗑️ Đã xóa message";

            case "REACT":
                String emoji = config.params.get("emoji");
                if (emoji != null) {
                    event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emoji)).queue();
                }
                return null;

            default:
                return "❌ Action không được hỗ trợ: " + config.action;
        }
    }

    // ==================== HELPER METHODS ====================

    private String addRole(Guild guild, Member member, String roleId) {
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return "❌ Role không tồn tại";
        }
        if (member.getRoles().contains(role)) {
            return "ℹ️ Bạn đã có role này rồi";
        }
        guild.addRoleToMember(member, role).queue();
        return "✅ Đã thêm role: " + role.getName();
    }

    private String removeRole(Guild guild, Member member, String roleId) {
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return "❌ Role không tồn tại";
        }
        if (!member.getRoles().contains(role)) {
            return "ℹ️ Bạn không có role này";
        }
        guild.removeRoleFromMember(member, role).queue();
        return "✅ Đã xóa role: " + role.getName();
    }

    private String toggleRole(Guild guild, Member member, String roleId) {
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return "❌ Role không tồn tại";
        }
        if (member.getRoles().contains(role)) {
            guild.removeRoleFromMember(member, role).queue();
            return "✅ Đã xóa role: " + role.getName();
        } else {
            guild.addRoleToMember(member, role).queue();
            return "✅ Đã thêm role: " + role.getName();
        }
    }

    // vote(...) removed - native Discord Polls handle voting

    private String createTicket(Guild guild, Member member, String type, String categoryId) {
        // TODO: Implement ticket creation logic
        log.info("Ticket creation requested: type={}, user={}, category={}", type, member.getId(), categoryId);
        return "🎫 Ticket system chưa được implement đầy đủ";
    }

    private String closeTicket(TextChannel channel) {
        if (channel.getName().startsWith("ticket-")) {
            channel.delete().queue();
            return "✅ Đã đóng ticket";
        }
        return "❌ Channel này không phải ticket";
    }

    private String lockChannel(TextChannel channel, Guild guild) {
        // TODO: Implement channel permission lock
        log.info("Channel lock requested: {}", channel.getId());
        return "🔒 Channel lock chưa được implement";
    }

    private String unlockChannel(TextChannel channel, Guild guild) {
        // TODO: Implement channel permission unlock
        log.info("Channel unlock requested: {}", channel.getId());
        return "🔓 Channel unlock chưa được implement";
    }

    private HandlerConfig parseHandlerConfig(JsonNode config) {
        try {
            HandlerConfig handler = new HandlerConfig();
            handler.action = config.get("action").asText();
            handler.params = new HashMap<>();

            if (config.has("params")) {
                config.get("params").fields().forEachRemaining(entry -> {
                    handler.params.put(entry.getKey(), entry.getValue().asText());
                });
            }

            if (config.has("response")) {
                handler.responseEphemeral = config.get("response").path("ephemeral").asBoolean(true);
                handler.responseContent = config.get("response").path("content").asText(null);
            }

            return handler;
        } catch (Exception e) {
            log.error("Failed to parse handler config: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Handler configuration class
     */
    public static class HandlerConfig {
        public String action;
        public Map<String, String> params = new HashMap<>();
        public boolean responseEphemeral = true;
        public String responseContent;

        public HandlerConfig() {}

        public HandlerConfig(String action, Map<String, String> params) {
            this.action = action;
            this.params = params != null ? params : new HashMap<>();
        }
    }
}
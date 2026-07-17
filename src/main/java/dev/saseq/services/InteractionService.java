package dev.saseq.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.poll.MessagePollVoteAddEvent;
import net.dv8tion.jda.api.events.message.poll.MessagePollVoteRemoveEvent;
import net.dv8tion.jda.api.events.message.poll.GenericMessagePollVoteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles Discord interaction events (buttons, select menus, modals, slash commands).
 * Stores pending interactions for MCP clients to poll and respond to.
 */
@Service
public class InteractionService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(InteractionService.class);

    private final JDA jda;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong interactionIdCounter = new AtomicLong(0);
    private final ComponentHandler componentHandler;
    private final PollService pollService;
    private final TicketService ticketService;

    // Store pending interactions: interactionToken -> InteractionData
    private final Map<String, InteractionData> pendingInteractions = new ConcurrentHashMap<>();
    // Store interaction metadata for listing
    private final Map<String, InteractionMeta> interactionMeta = new ConcurrentHashMap<>();

    public InteractionService(JDA jda, PollService pollService, TicketService ticketService) {
        this.jda = jda;
        this.pollService = pollService;
        this.ticketService = ticketService;
        this.componentHandler = new ComponentHandler();
    }

    @PostConstruct
    public void init() {
        jda.addEventListener(this);
        log.info("InteractionService registered with JDA");
    }

    @PreDestroy
    public void cleanup() {
        jda.removeEventListener(this);
        pendingInteractions.clear();
        interactionMeta.clear();
        log.info("InteractionService unregistered from JDA");
    }

    // ==================== Event Listeners ====================

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String customId = event.getComponentId();

        try {
            // CRITICAL: Defer IMMEDIATELY to avoid 3-second timeout
            // This must be the FIRST thing we do for ALL button interactions
            event.deferReply(true).queue();

            // Now process the interaction with the deferred token
            // === SPECIAL HANDLERS (ưu tiên cao nhất) ===
            if (customId.equals("create_poll_demo")) {
                TextChannel ch = event.getChannel().asTextChannel();
                // Now creates a real native Discord Poll (not custom embed+buttons)
                String result = pollService.createPoll(ch.getId(), "Bạn thích Discord MCP Fork không?",
                        java.util.Arrays.asList("Rất thích", "Thích", "Bình thường", "Không thích"), "60");
                event.getHook().sendMessage("📊 Native Discord Poll đã tạo!\n" + result).setEphemeral(true).queue();
                log.info("Created native poll demo");
                return;
            }

            if (customId.startsWith("open_modal:")) {
                String modalId = customId.substring("open_modal:".length());
                event.getHook().sendMessage("OPEN_MODAL:" + modalId + " (cần Hermes respond_with_modal)").setEphemeral(true).queue();
                log.info("Modal trigger: {}", modalId);
                return;
            }

            // === CONVENTION HANDLERS ===
            if (componentHandler.matchesConvention(customId)) {
                String response = componentHandler.executeConvention(event);

                // Override cho ticket_create (vì ComponentHandler chỉ có stub)
                if (customId.startsWith("ticket_create:")) {
                    String type = customId.substring("ticket_create:".length());
                    String reason = null;
                    String guildId = event.getGuild().getId();
                    String userId = event.getMember().getId();
                    String ticketId = ticketService.createTicket(guildId, userId, type, reason);
                    response = "🎫 Ticket đã tạo: #" + ticketId;
                } else if (customId.equals("ticket_close")) {
                    String guildId = event.getGuild().getId();
                    String channelId = event.getChannel().asTextChannel().getId();
                    String userId = event.getMember().getId();
                    response = ticketService.closeTicket(guildId, channelId, userId);
                }
                // NOTE: vote:* handling removed - we now use native Discord Polls

                if (response != null) {
                    final String finalResponse = response;
                    event.getHook().sendMessage(finalResponse).setEphemeral(true).queue(
                            success -> log.info("Sent convention response: {}", finalResponse),
                            error -> log.error("Failed to send convention response: {}", error.getMessage())
                    );
                }
                log.info("Handled convention button: {} by {}", customId, event.getUser().getName());
                return;
            }

            // Kiểm tra registry
            ComponentHandler.HandlerConfig handler = componentHandler.getHandler(customId);
            if (handler != null) {
                String response = componentHandler.executeRegistry(event, handler);
                if (response != null && handler.responseContent != null) {
                    event.getHook().sendMessage(handler.responseContent).setEphemeral(handler.responseEphemeral).queue();
                } else if (response != null) {
                    event.getHook().sendMessage(response).setEphemeral(true).queue();
                }
                log.info("Handled registry button: {} by {}", customId, event.getUser().getName());
                return;
            }

            // Fallback: lưu interaction để Hermes poll
            storeInteraction(event, InteractionType.BUTTON);
            log.info("Stored button interaction for Hermes: {}", customId);
        } catch (Exception e) {
            log.error("Error handling button interaction {}: {}", customId, e.getMessage(), e);
            try {
                // Use hook to send error since we already deferred
                event.getHook().sendMessage("❌ Lỗi xử lý button: " + e.getMessage()).setEphemeral(true).queue();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        // Defer early so AI has up to 15min to respond with rich content
        event.deferReply(true).queue();
        storeInteraction(event, InteractionType.SELECT_MENU);
        log.info("Stored select menu interaction for Hermes: {}", event.getComponentId());
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        // Modal submit - defer so we can reply with rich content or followup
        event.deferReply(true).queue();
        storeInteraction(event, InteractionType.MODAL);
        log.info("Stored modal submit interaction: {}", event.getModalId());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        storeInteraction(event, InteractionType.SLASH_COMMAND);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        // Autocomplete has a very short timeout (~3s recommended), store immediately
        storeInteraction(event, InteractionType.AUTOCOMPLETE);
        log.info("Stored autocomplete for command: {} (focused: {})", event.getName(), event.getFocusedOption().getName());
    }

    @Override
    public void onGenericComponentInteractionCreate(GenericComponentInteractionCreateEvent event) {
        // Fallback for other component types (EntitySelect, etc.)
        if (!(event instanceof ButtonInteractionEvent) && !(event instanceof StringSelectInteractionEvent)) {
            storeInteraction(event, InteractionType.COMPONENT);
        }
    }

    // ==================== Native Discord Poll Vote Events ====================

    @Override
    public void onMessagePollVoteAdd(MessagePollVoteAddEvent event) {
        storePollVote(event, true);
    }

    @Override
    public void onMessagePollVoteRemove(MessagePollVoteRemoveEvent event) {
        storePollVote(event, false);
    }

    private void storePollVote(GenericMessagePollVoteEvent event, boolean isAdd) {
        try {
            String token = event.getMessageId() + ":" + event.getUserId() + ":" + System.currentTimeMillis(); // pseudo token
            String id = event.getMessageId();

            InteractionData data = new InteractionData();
            data.id = id;
            data.token = token;
            data.type = InteractionType.POLL_VOTE;
            data.guildId = event.getGuild() != null ? event.getGuild().getId() : null;
            data.channelId = event.getChannel().getId();
            data.userId = event.getUserId();
            // username may require retrieve, we can do lazy or use id for now
            data.username = event.getUserId();
            data.timestamp = java.time.OffsetDateTime.now();

            data.commandName = isAdd ? "POLL_VOTE_ADD" : "POLL_VOTE_REMOVE";
            data.pollMessageId = event.getMessageId();
            data.pollAnswerId = String.valueOf(event.getAnswerId());

            pendingInteractions.put(token, data);

            InteractionMeta meta = new InteractionMeta();
            meta.id = id;
            meta.token = token;
            meta.type = "POLL_VOTE";
            meta.guildId = data.guildId;
            meta.channelId = data.channelId;
            meta.userId = data.userId;
            meta.username = data.username;
            meta.commandName = data.commandName;
            meta.componentId = "poll_answer_" + event.getAnswerId();
            meta.timestamp = data.timestamp;
            meta.responded = false;
            interactionMeta.put(token, meta);

            log.info("Stored native poll vote: {} by user {} on answer {}", isAdd ? "ADD" : "REMOVE", data.userId, event.getAnswerId());
        } catch (Exception e) {
            log.error("Failed to store poll vote event", e);
        }
    }

    private void storeInteraction(GenericInteractionCreateEvent event, InteractionType type) {
        String token = event.getToken();
        String id = event.getId(); // JDA 6.x uses getIdLong() via ISnowflake

        // FIX: Some interactions (esp. GenericComponent fallback) can return null token from JDA.
        // ConcurrentHashMap does not allow null keys → causes NPE on hashCode later.
        if (token == null || token.isBlank()) {
            token = "fallback:" + id + ":" + System.currentTimeMillis();
            log.warn("Interaction token was null/blank for type={}, using fallback token: {}", type, token);
        }

        // Create interaction data
        InteractionData data = new InteractionData();
        data.id = id;
        data.token = token;
        data.type = type;
        data.guildId = event.getGuild() != null ? event.getGuild().getId() : null;
        data.channelId = event.getChannel() != null ? event.getChannel().getId() : null;
        data.userId = event.getUser().getId();
        data.username = event.getUser().getName();
        data.timestamp = OffsetDateTime.now();

        // Type-specific data AND get the hook from specific event types
        switch (type) {
            case BUTTON -> {
                ButtonInteractionEvent buttonEvent = (ButtonInteractionEvent) event;
                data.hook = buttonEvent.getHook();
                data.componentId = buttonEvent.getComponentId();
                // Button component has getCustomId() via ICustomId
                net.dv8tion.jda.api.components.buttons.Button button = buttonEvent.getButton();
                if (button != null) {
                    data.buttonId = button.getCustomId();
                }
            }
            case SELECT_MENU -> {
                StringSelectInteractionEvent selectEvent = (StringSelectInteractionEvent) event;
                data.hook = selectEvent.getHook();
                data.componentId = selectEvent.getComponentId();
                data.selectedValues = selectEvent.getValues();
            }
            case MODAL -> {
                ModalInteractionEvent modalEvent = (ModalInteractionEvent) event;
                data.hook = modalEvent.getHook();
                data.customId = modalEvent.getModalId();
                data.modalComponents = parseModalComponents(modalEvent);
            }
            case SLASH_COMMAND -> {
                SlashCommandInteractionEvent slashEvent = (SlashCommandInteractionEvent) event;
                data.hook = slashEvent.getHook();
                data.commandName = slashEvent.getName();
                data.commandOptions = parseCommandOptions(slashEvent);
            }
            case AUTOCOMPLETE -> {
                CommandAutoCompleteInteractionEvent autoEvent = (CommandAutoCompleteInteractionEvent) event;
                data.hook = null; // Autocomplete responds via replyChoices, not hook/edit
                data.commandName = autoEvent.getName();
                data.commandOptions = parseCommandOptionsFromAuto(autoEvent);
                data.customId = autoEvent.getFocusedOption().getName(); // the option being autocompleted
            }
            case COMPONENT -> {
                GenericComponentInteractionCreateEvent compEvent = (GenericComponentInteractionCreateEvent) event;
                data.hook = compEvent.getHook();
                data.componentId = compEvent.getComponentId();
                data.componentType = compEvent.getComponentType().name();
            }
        }

        // CRITICAL FIX: store the original event so respond tools can use it reliably
        data.event = event;

        pendingInteractions.put(token, data);

        // Store metadata for listing
        InteractionMeta meta = new InteractionMeta();
        meta.id = id;
        meta.token = token;
        meta.type = type.name();
        meta.guildId = data.guildId;
        meta.channelId = data.channelId;
        meta.userId = data.userId;
        meta.username = data.username;
        meta.customId = data.customId;
        meta.commandName = data.commandName;
        meta.componentId = data.componentId;
        meta.componentType = data.componentType;
        meta.timestamp = data.timestamp;
        meta.responded = false;
        interactionMeta.put(token, meta);

        log.info("Stored interaction: type={}, id={}, user={}, customId={}, command={}",
                type, id, data.username, data.customId, data.commandName);
    }

    private List<Map<String, Object>> parseModalComponents(ModalInteractionEvent event) {
        List<Map<String, Object>> components = new ArrayList<>();
        // ModalInteraction has getValues() returning List<ModalMapping>
        for (ModalMapping mapping : event.getValues()) {
            Map<String, Object> comp = new ConcurrentHashMap<>();
            comp.put("type", mapping.getType().name());
            comp.put("custom_id", mapping.getCustomId());
            comp.put("value", mapping.getAsString());
            components.add(comp);
        }
        return components;
    }

    private List<Map<String, Object>> parseCommandOptions(SlashCommandInteractionEvent event) {
        List<Map<String, Object>> options = new ArrayList<>();
        event.getOptions().forEach(option -> {
            Map<String, Object> opt = new ConcurrentHashMap<>();
            opt.put("name", option.getName());
            opt.put("type", option.getType().name());
            opt.put("value", option.getAsString());
            options.add(opt);
        });
        return options;
    }

    private List<Map<String, Object>> parseCommandOptionsFromAuto(CommandAutoCompleteInteractionEvent event) {
        List<Map<String, Object>> options = new ArrayList<>();
        event.getOptions().forEach(option -> {
            Map<String, Object> opt = new ConcurrentHashMap<>();
            opt.put("name", option.getName());
            opt.put("type", option.getType().name());
            opt.put("value", option.getAsString());
            options.add(opt);
        });
        return options;
    }

    // ==================== MCP Tools ====================

    /**
     * List all pending interactions that haven't been responded to yet.
     */
    @Tool(name = "list_pending_interactions", description = "List all pending Discord interactions waiting for response")
    public String listPendingInteractions(@ToolParam(description = "Maximum number to return (default 50)", required = false) String limit) {
        int max = 50;
        if (limit != null && !limit.isBlank()) {
            try { max = Integer.parseInt(limit); } catch (NumberFormatException ignored) {}
        }

        List<InteractionMeta> pending = interactionMeta.values().stream()
                .filter(m -> !m.responded)
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(max)
                .toList();

        if (pending.isEmpty()) {
            return "No pending interactions";
        }

        StringBuilder sb = new StringBuilder();
                sb.append("**Pending Interactions (").append(pending.size()).append("):**\\n");
                for (InteractionMeta meta : pending) {
                    String extra = "";
                    if ("POLL_VOTE".equals(meta.type)) {
                        extra = " | PollMsg: " + (meta.componentId != null ? meta.componentId : "N/A");
                    }
                    sb.append(String.format(
                            "- ID: `%s` | Token: `%s` | Type: %s | User: %s | CustomID: %s | Command: %s | Component: %s | Time: %s%s\\n",
                            meta.id, meta.token, meta.type, meta.username, meta.customId != null ? meta.customId : "N/A",
                            meta.commandName != null ? meta.commandName : "N/A",
                            meta.componentId != null ? meta.componentId : "N/A", meta.timestamp, extra
                    ));
                }
                return sb.toString();
    }

    /**
     * Get detailed information about a specific pending interaction.
     */
    @Tool(name = "get_interaction", description = "Get full details of a specific pending interaction by token")
    public String getInteraction(@ToolParam(description = "Interaction token (from list_pending_interactions)") String token) {
        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("id", data.id);
            json.put("token", data.token);
            json.put("type", data.type.name());
            json.put("guildId", data.guildId);
            json.put("channelId", data.channelId);
            json.put("userId", data.userId);
            json.put("username", data.username);
            json.put("timestamp", data.timestamp.toString());
            json.put("customId", data.customId);
            json.put("commandName", data.commandName);
            json.put("componentId", data.componentId);
            json.put("buttonId", data.buttonId);
            json.put("componentType", data.componentType);
            json.put("pollMessageId", data.pollMessageId);
            json.put("pollAnswerId", data.pollAnswerId);
            json.set("selectedValues", objectMapper.valueToTree(data.selectedValues));
            json.set("modalComponents", objectMapper.valueToTree(data.modalComponents));
            json.set("commandOptions", objectMapper.valueToTree(data.commandOptions));

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return "Error serializing interaction: " + e.getMessage();
        }
    }

    /**
     * Respond to an interaction (acknowledge with message).
     * Supports full rich content (embeds + buttons/selects).
     * For most button/select flows the listener already defers, so this often edits the deferred reply.
     */
    @Tool(name = "respond_interaction", description = "Respond to pending interaction (button/select/slash/modal) with text + embeds + components. Rich support enabled.")
    public String respondInteraction(
            @ToolParam(description = "Interaction token") String token,
            @ToolParam(description = "Response message content", required = false) String content,
            @ToolParam(description = "JSON array of embeds (optional)", required = false) String embedsJson,
            @ToolParam(description = "JSON array of components/buttons/selects (optional)", required = false) String componentsJson,
            @ToolParam(description = "Whether response is ephemeral (only visible to user)", required = false) String ephemeral) {

        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            boolean isEphemeral = Boolean.parseBoolean(ephemeral != null ? ephemeral : "false");
            List<MessageEmbed> embeds = MessageService.parseEmbeds(embedsJson);
            List<ActionRow> components = MessageService.parseComponents(componentsJson);

            GenericInteractionCreateEvent gEvent = data.event;
            InteractionHook hook = data.hook;
            if (hook == null && gEvent != null) {
                // hook should have been captured in storeInteraction per type
                if (gEvent instanceof ButtonInteractionEvent be) hook = be.getHook();
                else if (gEvent instanceof StringSelectInteractionEvent se) hook = se.getHook();
                else if (gEvent instanceof ModalInteractionEvent me) hook = me.getHook();
                else if (gEvent instanceof SlashCommandInteractionEvent sl) hook = sl.getHook();
                else if (gEvent instanceof GenericComponentInteractionCreateEvent ce) hook = ce.getHook();
            }

            if (hook != null) {
                // Preferred path after defer (most stored interactions)
                var edit = hook.editOriginal(content != null && !content.isBlank() ? content : " ");
                if (!embeds.isEmpty()) edit.setEmbeds(embeds);
                if (!components.isEmpty()) edit.setComponents(components);
                edit.queue(
                    success -> markResponded(token),
                    err -> log.error("editOriginal failed: {}", err.getMessage())
                );
                return "Interaction responded (via hook editOriginal) with rich content.";
            }

            if (gEvent == null) {
                return "No hook or event available to respond";
            }

            Interaction interaction = gEvent.getInteraction();

            // Initial reply path (if not deferred yet)
            ReplyCallbackAction replyAction;
            String safeContent = (content != null && !content.isBlank()) ? content : " ";

            if (interaction instanceof SlashCommandInteractionEvent slash) {
                replyAction = isEphemeral ? slash.reply(safeContent).setEphemeral(true) : slash.reply(safeContent);
            } else if (interaction instanceof ComponentInteraction comp) {
                replyAction = isEphemeral ? comp.reply(safeContent).setEphemeral(true) : comp.reply(safeContent);
            } else if (interaction instanceof ModalInteraction modal) {
                replyAction = isEphemeral ? modal.reply(safeContent).setEphemeral(true) : modal.reply(safeContent);
            } else {
                return "Unsupported interaction type for direct reply";
            }

            if (!embeds.isEmpty()) replyAction.addEmbeds(embeds);
            if (!components.isEmpty()) replyAction.addComponents(components);

            replyAction.queue(
                success -> markResponded(token),
                error -> log.error("Failed direct reply: {}", error.getMessage())
            );

            return "Interaction responded successfully with rich content (embeds + components).";
        } catch (Exception e) {
            log.error("Error in respondInteraction", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Defer an interaction response (acknowledge without sending content yet).
     * Critical for long processing. After defer use edit_interaction_response or followup.
     */
    @Tool(name = "defer_interaction", description = "Defer interaction (gives you time). Then edit or followup with rich content.")
    public String deferInteraction(
            @ToolParam(description = "Interaction token") String token,
            @ToolParam(description = "Whether response is ephemeral", required = false) String ephemeral) {

        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            boolean isEphemeral = Boolean.parseBoolean(ephemeral != null ? ephemeral : "false");
            GenericInteractionCreateEvent gEvent = data.event != null ? data.event : null;

            if (gEvent != null) {
                if (gEvent.getInteraction() instanceof SlashCommandInteractionEvent slash) {
                    (isEphemeral ? slash.deferReply(true) : slash.deferReply()).queue(
                        s -> markResponded(token),
                        e -> log.error("defer slash failed: {}", e.getMessage())
                    );
                } else if (gEvent.getInteraction() instanceof ComponentInteraction comp) {
                    (isEphemeral ? comp.deferReply(true) : comp.deferReply()).queue(
                        s -> markResponded(token),
                        e -> log.error("defer comp failed: {}", e.getMessage())
                    );
                } else if (gEvent.getInteraction() instanceof ModalInteraction modal) {
                    (isEphemeral ? modal.deferReply(true) : modal.deferReply()).queue(
                        s -> markResponded(token),
                        e -> log.error("defer modal failed: {}", e.getMessage())
                    );
                } else {
                    return "Cannot defer this interaction type";
                }
                return "Deferred successfully. Use edit_interaction_response or followup_interaction (supports embeds + components).";
            }

            return "No event available to defer";
        } catch (Exception e) {
            log.error("Error deferring interaction", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Edit the original interaction response (after defer or initial respond).
     * Now supports updating embeds and components (e.g. progress bar or button state change).
     */
    @Tool(name = "edit_interaction_response", description = "Edit original interaction response. Supports content + embeds + components (great for progress updates).")
    public String editInteractionResponse(
            @ToolParam(description = "Interaction token") String token,
            @ToolParam(description = "New message content (optional)", required = false) String content,
            @ToolParam(description = "JSON array of embeds (optional)", required = false) String embedsJson,
            @ToolParam(description = "JSON array of components/buttons (optional)", required = false) String componentsJson) {

        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            InteractionHook hook = data.hook;
            if (hook == null && data.event != null) {
                var ev = data.event;
                if (ev instanceof ButtonInteractionEvent be) hook = be.getHook();
                else if (ev instanceof StringSelectInteractionEvent se) hook = se.getHook();
                else if (ev instanceof ModalInteractionEvent me) hook = me.getHook();
                else if (ev instanceof SlashCommandInteractionEvent sl) hook = sl.getHook();
                else if (ev instanceof GenericComponentInteractionCreateEvent ce) hook = ce.getHook();
            }
            if (hook == null) {
                return "Interaction hook not available (defer first?)";
            }

            List<MessageEmbed> embeds = MessageService.parseEmbeds(embedsJson);
            List<ActionRow> comps = MessageService.parseComponents(componentsJson);

            var action = hook.editOriginal( (content != null && !content.isBlank()) ? content : " " );
            if (!embeds.isEmpty()) action.setEmbeds(embeds);
            if (!comps.isEmpty()) action.setComponents(comps);

            action.queue(
                success -> {},
                error -> log.error("Failed edit_interaction_response: {}", error.getMessage())
            );

            return "Interaction response edited with rich content (embeds/components supported).";
        } catch (Exception e) {
            log.error("Error editing interaction response", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Send a followup message to an interaction (after defer).
     * Can be used multiple times. Supports rich embeds + components.
     */
    @Tool(name = "followup_interaction", description = "Send followup after defer. Supports content + embeds + components. Can call many times.")
    public String followupInteraction(
            @ToolParam(description = "Interaction token") String token,
            @ToolParam(description = "Message content", required = false) String content,
            @ToolParam(description = "JSON array of embeds (optional)", required = false) String embedsJson,
            @ToolParam(description = "JSON array of components (optional)", required = false) String componentsJson,
            @ToolParam(description = "Whether followup is ephemeral", required = false) String ephemeral) {

        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            InteractionHook hook = data.hook;
            if (hook == null && data.event != null) {
                var ev = data.event;
                if (ev instanceof ButtonInteractionEvent be) hook = be.getHook();
                else if (ev instanceof StringSelectInteractionEvent se) hook = se.getHook();
                else if (ev instanceof ModalInteractionEvent me) hook = me.getHook();
                else if (ev instanceof SlashCommandInteractionEvent sl) hook = sl.getHook();
                else if (ev instanceof GenericComponentInteractionCreateEvent ce) hook = ce.getHook();
            }
            if (hook == null) {
                return "Interaction hook not available (defer first)";
            }

            boolean isEphemeral = Boolean.parseBoolean(ephemeral != null ? ephemeral : "false");
            List<MessageEmbed> embeds = MessageService.parseEmbeds(embedsJson);
            List<ActionRow> comps = MessageService.parseComponents(componentsJson);

            var action = hook.sendMessage( (content != null && !content.isBlank()) ? content : " " );
            if (isEphemeral) action.setEphemeral(true);
            if (!embeds.isEmpty()) action.addEmbeds(embeds);
            if (!comps.isEmpty()) action.addComponents(comps);

            action.queue(
                success -> {},
                error -> log.error("followup failed: {}", error.getMessage())
            );

            return "Followup sent successfully (rich content supported).";
        } catch (Exception e) {
            log.error("Error sending followup", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete the original interaction response.
     */
    @Tool(name = "delete_interaction_response", description = "Delete the original interaction response message")
    public String deleteInteractionResponse(@ToolParam(description = "Interaction token") String token) {
        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            InteractionHook hook = data.hook;
            if (hook == null) {
                return "Interaction hook not available";
            }

            hook.deleteOriginal().queue(
                    success -> {},
                    error -> log.error("Failed to delete interaction response: ", error)
            );

            return "Interaction response deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting interaction response", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Respond to an autocomplete interaction with choices.
     */
    @Tool(name = "respond_autocomplete", description = "Respond to a slash command autocomplete with suggested choices.")
    public String respondAutocomplete(
            @ToolParam(description = "Interaction token") String token,
            @ToolParam(description = "JSON array of choices: [{\"name\": \"Option A\", \"value\": \"a\"}, ...]") String choicesJson) {

        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            GenericInteractionCreateEvent gEvent = data.event;
            if (gEvent == null) {
                return "Interaction event no longer available";
            }

            if (!(gEvent.getInteraction() instanceof CommandAutoCompleteInteractionEvent autoEvent)) {
                return "This is not an autocomplete interaction";
            }

            List<net.dv8tion.jda.api.interactions.commands.Command.Choice> choices = parseAutocompleteChoices(choicesJson);

            autoEvent.replyChoices(choices).queue(
                success -> markResponded(token),
                error -> log.error("Failed to respond to autocomplete: {}", error.getMessage())
            );

            return "Autocomplete choices sent successfully.";
        } catch (Exception e) {
            log.error("Error responding to autocomplete", e);
            return "Error: " + e.getMessage();
        }
    }

    private List<net.dv8tion.jda.api.interactions.commands.Command.Choice> parseAutocompleteChoices(String choicesJson) {
        List<net.dv8tion.jda.api.interactions.commands.Command.Choice> choices = new ArrayList<>();
        if (choicesJson == null || choicesJson.isBlank()) return choices;

        try {
            JsonNode arr = objectMapper.readTree(choicesJson);
            if (!arr.isArray()) arr = objectMapper.createArrayNode().add(arr);

            for (JsonNode c : arr) {
                String name = c.path("name").asText(null);
                String value = c.path("value").asText(null);
                if (name != null && value != null) {
                    choices.add(new net.dv8tion.jda.api.interactions.commands.Command.Choice(name, value));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse autocomplete choices", e);
        }
        return choices;
    }

    /**
     * Respond to a component interaction with a modal (for buttons/selects that should open a modal).
     * Now properly builds the modal from componentsJson (TextInputs).
     */
    @Tool(name = "respond_with_modal", description = "Open a modal form in response to button/select/slash. Provide componentsJson with TextInputs.")
    public String respondWithModal(
            @ToolParam(description = "Interaction token") String token,
            @ToolParam(description = "Modal custom ID") String customId,
            @ToolParam(description = "Modal title") String title,
            @ToolParam(description = "JSON array of ActionRow with TextInput (type 4) components") String componentsJson) {

        if (token == null || token.isBlank()) {
            return "Invalid token: token cannot be null or empty";
        }
        InteractionData data = pendingInteractions.get(token);
        if (data == null) {
            return "Interaction not found or already responded: " + token;
        }

        try {
            GenericInteractionCreateEvent gEvent = data.event;
            if (gEvent == null) {
                return "Interaction event no longer available";
            }

            Interaction interaction = gEvent.getInteraction();

            // Build real modal using ModalService logic or inline
            net.dv8tion.jda.api.modals.Modal.Builder modalBuilder = net.dv8tion.jda.api.modals.Modal.create(customId, title);

            // Parse using ModalService's approach (reuse simple parsing here)
            List<net.dv8tion.jda.api.components.ModalTopLevelComponent> modalComps = parseModalTopLevelForResponse(componentsJson);
            if (!modalComps.isEmpty()) {
                modalBuilder.addComponents(modalComps.toArray(new net.dv8tion.jda.api.components.ModalTopLevelComponent[0]));
            }

            net.dv8tion.jda.api.modals.Modal modal = modalBuilder.build();

            if (interaction instanceof ComponentInteraction compEvent) {
                compEvent.replyModal(modal).queue(
                    success -> markResponded(token),
                    error -> log.error("replyModal failed: {}", error.getMessage())
                );
            } else if (interaction instanceof SlashCommandInteractionEvent slashEvent) {
                slashEvent.replyModal(modal).queue(
                    success -> markResponded(token),
                    error -> log.error("replyModal (slash) failed: {}", error.getMessage())
                );
            } else {
                return "This interaction type cannot open a modal";
            }

            return "Modal opened successfully.";
        } catch (Exception e) {
            log.error("Error creating modal response", e);
            return "Error: " + e.getMessage();
        }
    }

    // Lightweight modal component parser for respond_with_modal (TextInput only)
    private List<net.dv8tion.jda.api.components.ModalTopLevelComponent> parseModalTopLevelForResponse(String componentsJson) {
        List<net.dv8tion.jda.api.components.ModalTopLevelComponent> out = new ArrayList<>();
        if (componentsJson == null || componentsJson.isBlank()) return out;
        try {
            JsonNode arr = objectMapper.readTree(componentsJson);
            if (!arr.isArray()) arr = objectMapper.createArrayNode().add(arr);

            for (JsonNode row : arr) {
                JsonNode comps = row.has("components") ? row.get("components") : row;
                for (JsonNode c : comps) {
                    if (c.path("type").asInt(0) == 4) { // TextInput
                        String cid = c.path("custom_id").asText(null);
                        if (cid == null) continue;
                        int styleV = c.path("style").asInt(1);
                        var style = styleV == 2 ? net.dv8tion.jda.api.components.textinput.TextInputStyle.PARAGRAPH : net.dv8tion.jda.api.components.textinput.TextInputStyle.SHORT;
                        var builder = net.dv8tion.jda.api.components.textinput.TextInput.create(cid, style)
                            .setRequired(c.path("required").asBoolean(true))
                            .setMinLength(c.path("min_length").asInt(0))
                            .setMaxLength(c.path("max_length").asInt(4000));
                        if (c.hasNonNull("placeholder")) builder = builder.setPlaceholder(c.get("placeholder").asText());
                        if (c.hasNonNull("value")) builder = builder.setValue(c.get("value").asText());

                        net.dv8tion.jda.api.components.textinput.TextInput ti = builder.build();

                        // Wrap in Label (required for JDA 6 modals)
                        String lbl = c.path("label").asText("Input");
                        out.add(net.dv8tion.jda.api.components.label.Label.of(lbl, ti));
                    }
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void markResponded(String token) {
        InteractionData data = pendingInteractions.remove(token);
        InteractionMeta meta = interactionMeta.get(token);
        if (meta != null) {
            meta.responded = true;
        }
        if (data != null) {
            log.info("Interaction marked as responded: {}", token);
        }
    }

    // ==================== NEW TOOLS: Slash Command + Permission + Audit ====================

    /**
     * Register a slash command with full options support (Phase 3).
     * Supports STRING, INTEGER, BOOLEAN, USER, CHANNEL, ROLE, MENTIONABLE, ATTACHMENT options.
     */
    @Tool(name = "register_slash_command", description = "Register a Discord slash command with options, choices, required flags. Use optionsJson for full definition.")
    public String registerSlashCommand(
            @ToolParam(description = "Command name (lowercase, no spaces)") String name,
            @ToolParam(description = "Command description") String description,
            @ToolParam(description = "JSON array of options (optional). Example: [{\"name\":\"user\",\"description\":\"Target user\",\"type\":\"USER\",\"required\":true}]", required = false) String optionsJson,
            @ToolParam(description = "Guild ID (optional, leave empty for global)") String guildId) {
        try {
            var cmd = net.dv8tion.jda.api.interactions.commands.build.Commands.slash(name, description);

            if (optionsJson != null && !optionsJson.isBlank()) {
                List<net.dv8tion.jda.api.interactions.commands.build.OptionData> options = parseCommandOptionsJson(optionsJson);
                cmd.addOptions(options);
            }

            if (guildId != null && !guildId.isEmpty()) {
                Guild guild = jda.getGuildById(guildId);
                if (guild == null) return "Guild not found";
                guild.upsertCommand(cmd).queue();
                return "Slash command /" + name + " registered for guild " + guildId + " with " + (optionsJson != null ? "options" : "no options");
            } else {
                jda.upsertCommand(cmd).queue();
                return "Slash command /" + name + " registered globally with " + (optionsJson != null ? "options" : "no options");
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private List<net.dv8tion.jda.api.interactions.commands.build.OptionData> parseCommandOptionsJson(String optionsJson) {
        List<net.dv8tion.jda.api.interactions.commands.build.OptionData> optionDataList = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(optionsJson);
            if (!arr.isArray()) arr = objectMapper.createArrayNode().add(arr);

            for (JsonNode opt : arr) {
                String optName = opt.path("name").asText();
                String optDesc = opt.path("description").asText("No description");
                String typeStr = opt.path("type").asText("STRING").toUpperCase();

                net.dv8tion.jda.api.interactions.commands.OptionType type;
                try {
                    type = net.dv8tion.jda.api.interactions.commands.OptionType.valueOf(typeStr);
                } catch (Exception ex) {
                    type = net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
                }

                var optionData = new net.dv8tion.jda.api.interactions.commands.build.OptionData(type, optName, optDesc);
                optionData.setRequired(opt.path("required").asBoolean(false));

                if (opt.has("choices") && opt.get("choices").isArray()) {
                    for (JsonNode choice : opt.get("choices")) {
                        String cName = choice.path("name").asText();
                        String cValue = choice.path("value").asText();
                        optionData.addChoice(cName, cValue);
                    }
                }

                if (opt.has("autocomplete")) {
                    optionData.setAutoComplete(opt.path("autocomplete").asBoolean(false));
                }

                // Add min/max for numbers if provided
                if (type == net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER || type == net.dv8tion.jda.api.interactions.commands.OptionType.NUMBER) {
                    if (opt.has("min_value")) optionData.setMinValue(opt.get("min_value").asDouble());
                    if (opt.has("max_value")) optionData.setMaxValue(opt.get("max_value").asDouble());
                }

                optionDataList.add(optionData);
            }
        } catch (Exception e) {
            log.error("Failed to parse optionsJson for slash command", e);
            throw new IllegalArgumentException("Invalid optionsJson: " + e.getMessage());
        }
        return optionDataList;
    }

    /**
     * Check if a user has a specific role or permission (Phase 4 improved).
     */
    @Tool(name = "check_user_permission", description = "Check user role/permission. Use for pre-check before dangerous actions (buttons, slash, etc).")
    public String checkUserPermission(
            @ToolParam(description = "Guild ID") String guildId,
            @ToolParam(description = "User ID") String userId,
            @ToolParam(description = "Role ID to check (optional)") String roleId,
            @ToolParam(description = "Permission name (e.g. ADMINISTRATOR, MANAGE_SERVER, KICK_MEMBERS)", required = false) String permission,
            @ToolParam(description = "Channel ID for channel-specific permission check (optional)", required = false) String channelId) {
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return "Guild not found";
            Member member = guild.retrieveMemberById(userId).complete();
            if (member == null) return "Member not found";

            if (roleId != null && !roleId.isEmpty()) {
                boolean hasRole = member.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
                return hasRole ? "User has role " + roleId : "User does NOT have role " + roleId;
            }

            if (permission != null && !permission.isEmpty()) {
                net.dv8tion.jda.api.Permission perm = net.dv8tion.jda.api.Permission.valueOf(permission);
                boolean hasPerm;
                if (channelId != null && !channelId.isEmpty()) {
                    var channel = guild.getGuildChannelById(channelId);
                    hasPerm = channel != null && member.hasPermission(channel, perm);
                } else {
                    hasPerm = member.hasPermission(perm);
                }
                return hasPerm ? "User has permission " + permission : "User does NOT have permission " + permission;
            }
            return "No roleId or permission specified";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Check the bot's own permissions (useful for pre-check before performing actions).
     */
    @Tool(name = "check_bot_permission", description = "Check if the bot has a specific permission in guild or channel (Phase 4 - permission pre-check).")
    public String checkBotPermission(
            @ToolParam(description = "Guild ID") String guildId,
            @ToolParam(description = "Permission name (e.g. SEND_MESSAGES, MANAGE_MESSAGES, KICK_MEMBERS). Case insensitive.") String permission,
            @ToolParam(description = "Channel ID (optional, for channel-specific permission)", required = false) String channelId) {
        try {
            if (guildId == null || guildId.isBlank()) {
                return "guildId is required";
            }
            if (permission == null || permission.isBlank()) {
                return "permission is required (e.g. SEND_MESSAGES)";
            }

            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return "Guild not found";
            Member bot = guild.getSelfMember();

            // Normalize permission name (handle spaces, dashes, case)
            String normalized = permission.trim().toUpperCase().replace("-", "_").replace(" ", "_");

            // DEBUG: Print what we received
            System.out.println("[DEBUG check_bot_permission] Input permission: " + permission + ", normalized: " + normalized);

            // Map common Discord API permission names to JDA Permission enum names
            if (normalized.equals("MANAGE_MESSAGES")) {
                normalized = "MESSAGE_MANAGE";
                System.out.println("[DEBUG check_bot_permission] Mapped MANAGE_MESSAGES -> MESSAGE_MANAGE");
            }
            else if (normalized.equals("MANAGE_CHANNELS")) {
                normalized = "MANAGE_CHANNEL";
                System.out.println("[DEBUG check_bot_permission] Mapped MANAGE_CHANNELS -> MANAGE_CHANNEL");
            }
            else if (normalized.equals("SEND_MESSAGES")) {
                normalized = "MESSAGE_SEND";
                System.out.println("[DEBUG check_bot_permission] Mapped SEND_MESSAGES -> MESSAGE_SEND");
            }
            else if (normalized.equals("MANAGE_ROLES")) {
                normalized = "MANAGE_ROLES";
                System.out.println("[DEBUG check_bot_permission] MANAGE_ROLES (JDA uses same name)");
            }
            else if (normalized.equals("MANAGE_WEBHOOKS")) {
                normalized = "MANAGE_WEBHOOKS";
                System.out.println("[DEBUG check_bot_permission] MANAGE_WEBHOOKS (JDA uses same name)");
            }
            else if (normalized.equals("MANAGE_EMOJIS")) {
                normalized = "MANAGE_EMOJIS";
                System.out.println("[DEBUG check_bot_permission] MANAGE_EMOJIS (JDA uses same name)");
            }
            else if (normalized.equals("MANAGE_NICKNAMES")) {
                normalized = "MANAGE_NICKNAMES";
                System.out.println("[DEBUG check_bot_permission] MANAGE_NICKNAMES (JDA uses same name)");
            }
            else if (normalized.equals("VIEW_CHANNEL")) {
                normalized = "VIEW_CHANNEL";
                System.out.println("[DEBUG check_bot_permission] VIEW_CHANNEL (no mapping needed)");
            }
            else if (normalized.equals("VIEW_AUDIT_LOG")) {
                normalized = "VIEW_AUDIT_LOG";
                System.out.println("[DEBUG check_bot_permission] VIEW_AUDIT_LOG (no mapping needed)");
            }
            else if (normalized.equals("MANAGE_SERVER")) {
                normalized = "MANAGE_SERVER";
                System.out.println("[DEBUG check_bot_permission] MANAGE_SERVER (no mapping needed)");
            }
            else if (normalized.equals("MANAGE_THREADS")) {
                normalized = "THREAD_MANAGE";
                System.out.println("[DEBUG check_bot_permission] Mapped MANAGE_THREADS -> THREAD_MANAGE");
            }
            else if (normalized.equals("CREATE_PUBLIC_THREADS")) {
                normalized = "CREATE_PUBLIC_THREADS";
                System.out.println("[DEBUG check_bot_permission] CREATE_PUBLIC_THREADS (no mapping needed)");
            }
            else if (normalized.equals("CREATE_PRIVATE_THREADS")) {
                normalized = "CREATE_PRIVATE_THREADS";
                System.out.println("[DEBUG check_bot_permission] CREATE_PRIVATE_THREADS (no mapping needed)");
            }
            else if (normalized.equals("USE_EXTERNAL_EMOJIS")) {
                normalized = "USE_EXTERNAL_EMOJIS";
                System.out.println("[DEBUG check_bot_permission] USE_EXTERNAL_EMOJIS (no mapping needed)");
            }
            else if (normalized.equals("USE_EXTERNAL_STICKERS")) {
                normalized = "USE_EXTERNAL_STICKERS";
                System.out.println("[DEBUG check_bot_permission] USE_EXTERNAL_STICKERS (no mapping needed)");
            }
            else if (normalized.equals("USE_APPLICATION_COMMANDS")) {
                normalized = "USE_APPLICATION_COMMANDS";
                System.out.println("[DEBUG check_bot_permission] USE_APPLICATION_COMMANDS (no mapping needed)");
            }
            else if (normalized.equals("REQUEST_TO_SPEAK")) {
                normalized = "REQUEST_TO_SPEAK";
                System.out.println("[DEBUG check_bot_permission] REQUEST_TO_SPEAK (no mapping needed)");
            }
            else if (normalized.equals("ADMINISTRATOR")) {
                normalized = "ADMINISTRATOR";
                System.out.println("[DEBUG check_bot_permission] ADMINISTRATOR (no mapping needed)");
            }
            else {
                System.out.println("[DEBUG check_bot_permission] No mapping for: " + normalized);
            }

            net.dv8tion.jda.api.Permission perm;
            try {
                perm = net.dv8tion.jda.api.Permission.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return "Invalid permission name: " + permission + ". Use names like SEND_MESSAGES, MANAGE_MESSAGES, KICK_MEMBERS, etc.";
            }

            boolean has;
            if (channelId != null && !channelId.isEmpty()) {
                var ch = guild.getGuildChannelById(channelId);
                has = ch != null && bot.hasPermission(ch, perm);
            } else {
                has = bot.hasPermission(perm);
            }
            return has ? "Bot HAS permission " + permission : "Bot does NOT have permission " + permission;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get recent audit log entries (Phase 4 improvement).
     * Supports filtering by action type.
     */
    @Tool(name = "get_audit_logs", description = "Get recent audit log entries. Supports action filter (e.g. MEMBER_KICK, ROLE_CREATE).")
    public String getAuditLogs(
            @ToolParam(description = "Guild ID") String guildId,
            @ToolParam(description = "Max entries (default 10)") String limit,
            @ToolParam(description = "Filter by action type (optional, e.g. MEMBER_KICK, CHANNEL_CREATE)", required = false) String actionType) {
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return "Guild not found";
            int max = 10;
            if (limit != null && !limit.isBlank()) {
                try { max = Integer.parseInt(limit); } catch (Exception ignored) {}
            }

            var action = guild.retrieveAuditLogs().limit(max);
            if (actionType != null && !actionType.isBlank()) {
                try {
                    net.dv8tion.jda.api.audit.ActionType type = net.dv8tion.jda.api.audit.ActionType.valueOf(actionType.toUpperCase());
                    action = action.type(type);
                } catch (Exception ignored) {}
            }

            List<net.dv8tion.jda.api.audit.AuditLogEntry> entries = action.complete();
            StringBuilder sb = new StringBuilder();
            sb.append("**Recent Audit Logs");
            if (actionType != null) sb.append(" (filtered: ").append(actionType).append(")");
            sb.append(":**\n");

            for (net.dv8tion.jda.api.audit.AuditLogEntry e : entries) {
                sb.append(String.format("- %s | Action: %s | User: %s | Target: %s | Reason: %s\n",
                        e.getTimeCreated(),
                        e.getType().name(),
                        e.getUser() != null ? e.getUser().getName() : "N/A",
                        e.getTargetId(),
                        e.getReason() != null ? e.getReason() : "N/A"));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Internal Data Classes ====================

    private enum InteractionType {
        BUTTON, SELECT_MENU, MODAL, SLASH_COMMAND, COMPONENT, AUTOCOMPLETE, POLL_VOTE
    }

    private static class InteractionData {
        String id;
        String token;
        InteractionType type;
        String guildId;
        String channelId;
        String userId;
        String username;
        OffsetDateTime timestamp;
        String customId;
        String commandName;
        String componentId;
        String buttonId;
        String componentType;
        List<String> selectedValues = new ArrayList<>();
        List<Map<String, Object>> modalComponents = new ArrayList<>();
        List<Map<String, Object>> commandOptions = new ArrayList<>();

        // Poll vote specific
        String pollMessageId;
        String pollAnswerId;

        // Store the InteractionHook for responding (key fix)
        InteractionHook hook;

        // Store the original event for type-specific operations
        transient GenericInteractionCreateEvent event;

        GenericInteractionCreateEvent getEvent() {
            return event;
        }

        void setEvent(GenericInteractionCreateEvent event) {
            this.event = event;
        }
    }

    private static class InteractionMeta {
        String id;
        String token;
        String type;
        String guildId;
        String channelId;
        String userId;
        String username;
        String customId;
        String commandName;
        String componentId;
        String componentType;
        OffsetDateTime timestamp;
        boolean responded = false;
    }
}
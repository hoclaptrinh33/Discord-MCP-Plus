package dev.saseq.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Ticket service for creating and managing support tickets.
 * Creates private text channels for each ticket.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TICKETS_FILE = "tickets.json";

    // Ticket storage: ticketId -> TicketData
    private final Map<String, TicketData> tickets = new ConcurrentHashMap<>();

    // Category ID for tickets (configurable)
    private String ticketCategoryId;

    private final JDA jda;

    public TicketService(JDA jda) {
        this.jda = jda;
        loadTickets();
    }

    public void setTicketCategory(String categoryId) {
        this.ticketCategoryId = categoryId;
    }

    /**
     * Create a new ticket channel
     */
    @Tool(name = "create_ticket", description = "Create a new support ticket channel for a user")
    public String createTicket(@ToolParam(description = "Discord server ID") String guildId,
                               @ToolParam(description = "User ID to create ticket for") String userId,
                               @ToolParam(description = "Ticket type (e.g. support, bug, billing)") String type,
                               @ToolParam(description = "Reason for ticket", required = false) String reason) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return "❌ Guild not found: " + guildId;
        }
        Member member = guild.getMemberById(userId);
        if (member == null) {
            return "❌ Member not found in guild: " + userId;
        }
        String ticketId = UUID.randomUUID().toString().substring(0, 8);
        String channelName = "ticket-" + type.toLowerCase() + "-" + ticketId;

        // Find or create ticket category
        Category category = null;
        if (ticketCategoryId != null) {
            category = guild.getCategoryById(ticketCategoryId);
        }
        if (category == null) {
            // Try to find a category named "Tickets" or create one
            category = guild.getCategoriesByName("Tickets", true).stream().findFirst().orElse(null);
        }

        final Category finalCategory = category;
        final String finalTicketId = ticketId;

        // Create private text channel
        guild.createTextChannel(channelName, finalCategory)
                .addPermissionOverride(member, java.util.EnumSet.of(
                        net.dv8tion.jda.api.Permission.VIEW_CHANNEL,
                        net.dv8tion.jda.api.Permission.MESSAGE_SEND
                ), java.util.EnumSet.noneOf(net.dv8tion.jda.api.Permission.class))
                .addPermissionOverride(guild.getPublicRole(),
                        java.util.EnumSet.noneOf(net.dv8tion.jda.api.Permission.class),
                        java.util.EnumSet.of(net.dv8tion.jda.api.Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    // Send welcome message
                    String welcomeMsg = String.format(
                            "🎫 **Ticket #%s**\n\n" +
                                    "**User:** %s\n" +
                                    "**Type:** %s\n" +
                                    "**Reason:** %s\n\n" +
                                    "Staff will assist you shortly.\n" +
                                    "Use the button below to close this ticket.",
                            finalTicketId, member.getAsMention(), type, reason != null ? reason : "N/A"
                    );

                    channel.sendMessage(welcomeMsg)
                            .setComponents(ActionRow.of(Button.danger("ticket_close", "🔒 Close Ticket")))
                            .queue();

                    log.info("Ticket {} created in channel {}", finalTicketId, channel.getId());
                });

        // Store ticket data
        TicketData ticket = new TicketData();
        ticket.id = ticketId;
        ticket.userId = member.getId();
        ticket.username = member.getEffectiveName();
        ticket.type = type;
        ticket.reason = reason;
        ticket.createdAt = System.currentTimeMillis();
        ticket.status = "OPEN";

        tickets.put(ticketId, ticket);
        saveTickets();

        return ticketId;
    }

    /**
     * Close a ticket channel
     */
    @Tool(name = "close_ticket", description = "Close a support ticket by channel ID")
    public String closeTicket(@ToolParam(description = "Discord server ID") String guildId,
                              @ToolParam(description = "Ticket channel ID") String channelId,
                              @ToolParam(description = "User ID closing the ticket") String userId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return "❌ Guild not found: " + guildId;
        }
        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            return "❌ Channel not found: " + channelId;
        }
        Member member = guild.getMemberById(userId);
        if (member == null) {
            return "❌ Member not found in guild: " + userId;
        }

        String channelName = channel.getName();
        if (!channelName.startsWith("ticket-")) {
            return "❌ Channel này không phải ticket";
        }

        // Extract ticket ID from channel name
        String[] parts = channelName.split("-");
        String ticketId = parts.length >= 3 ? parts[parts.length - 1] : null;

        if (ticketId != null && tickets.containsKey(ticketId)) {
            tickets.get(ticketId).status = "CLOSED";
            tickets.get(ticketId).closedAt = System.currentTimeMillis();
            tickets.get(ticketId).closedBy = member.getId();
            saveTickets();
        }

        channel.delete().reason("Ticket closed by " + member.getEffectiveName()).queue();
        log.info("Ticket {} closed by {}", ticketId, member.getId());

        return "✅ Đã đóng ticket";
    }

    /**
     * List all open tickets
     */
    @Tool(name = "list_tickets", description = "List all open support tickets")
    public List<TicketData> listOpenTickets(@ToolParam(description = "Discord server ID", required = false) String guildId) {
        return tickets.values().stream()
                .filter(t -> "OPEN".equals(t.status))
                .sorted(Comparator.comparingLong(t -> t.createdAt))
                .toList();
    }

    private void loadTickets() {
        Path path = Paths.get(TICKETS_FILE);
        if (!Files.exists(path)) return;

        try {
            String content = Files.readString(path);
            if (content.isBlank()) return;

            var root = objectMapper.readTree(content);
            root.fields().forEachRemaining(entry -> {
                String ticketId = entry.getKey();
                var data = entry.getValue();

                TicketData ticket = new TicketData();
                ticket.id = ticketId;
                ticket.userId = data.get("userId").asText();
                ticket.username = data.get("username").asText();
                ticket.type = data.get("type").asText();
                ticket.reason = data.path("reason").asText(null);
                ticket.createdAt = data.get("createdAt").asLong();
                ticket.status = data.get("status").asText("OPEN");
                ticket.closedAt = data.path("closedAt").asLong(0);
                ticket.closedBy = data.path("closedBy").asText(null);

                tickets.put(ticketId, ticket);
            });

            log.info("Loaded {} tickets from tickets.json", tickets.size());
        } catch (Exception e) {
            log.error("Failed to load tickets: {}", e.getMessage());
        }
    }

    private void saveTickets() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            for (Map.Entry<String, TicketData> entry : tickets.entrySet()) {
                ObjectNode ticketNode = objectMapper.createObjectNode();
                ticketNode.put("userId", entry.getValue().userId);
                ticketNode.put("username", entry.getValue().username);
                ticketNode.put("type", entry.getValue().type);
                ticketNode.put("reason", entry.getValue().reason);
                ticketNode.put("createdAt", entry.getValue().createdAt);
                ticketNode.put("status", entry.getValue().status);
                ticketNode.put("closedAt", entry.getValue().closedAt);
                ticketNode.put("closedBy", entry.getValue().closedBy);
                root.set(entry.getKey(), ticketNode);
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(TICKETS_FILE), root);
        } catch (IOException e) {
            log.error("Failed to save tickets: {}", e.getMessage());
        }
    }

    public static class TicketData {
        public String id;
        public String userId;
        public String username;
        public String type;
        public String reason;
        public long createdAt;
        public String status; // OPEN, CLOSED
        public long closedAt;
        public String closedBy;
    }
}

package dev.saseq.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class MessageService {

    private final JDA jda;

    public MessageService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Helper method to get a MessageChannel by ID, checking both text channels and thread channels.
     */
    private MessageChannel getMessageChannelById(String channelId) {
        // First try text channel
        TextChannel textChannel = jda.getTextChannelById(channelId);
        if (textChannel != null) {
            return textChannel;
        }
        // Then try news/announcement channel
        NewsChannel newsChannel = jda.getNewsChannelById(channelId);
        if (newsChannel != null) {
            return newsChannel;
        }
        // Then try thread channel
        ThreadChannel threadChannel = jda.getThreadChannelById(channelId);
        if (threadChannel != null) {
            return threadChannel;
        }
        return null;
    }

    /**
     * Sends a message to a specified Discord channel.
     * Supports rich content: plain text + full embeds + buttons/select menus + file attachments (path preferred).
     *
     * @param channelId    The ID of the channel where the message will be sent.
     * @param message      Optional text content.
     * @param embedsJson   Optional JSON array of embed objects (title, description, fields, color, author, thumbnail, image, footer...).
     * @param componentsJson Optional JSON array of Action Rows with buttons (type 2) or string selects (type 3).
     * @param filesJson    Optional JSON array of files. Prefer "path" (local filesystem). Also supports "base64" or "url".
     * @return A confirmation message with a link to the sent message.
     */
    @Tool(name = "send_message", description = "Send a message (text + full embed + buttons/select + file upload by path). Supports rich Discord messages.")
    public String sendMessage(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Message content (text)", required = false) String message,
                              @ToolParam(description = "JSON array of embeds (title, description, fields, color, author, image, thumbnail, footer...)", required = false) String embedsJson,
                              @ToolParam(description = "JSON array of ActionRow components (buttons type=2 or string selects type=3)", required = false) String componentsJson,
                              @ToolParam(description = "JSON array of files. Preferred: [{\"path\":\"C:\\\\backup.zip\"}]. Also base64 or url supported.", required = false) String filesJson) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }

        List<MessageEmbed> embeds = parseEmbeds(embedsJson);
        List<ActionRow> components = parseComponents(componentsJson);
        List<FileUpload> files = parseFiles(filesJson);

        Message sentMessage;
        try {
            if (!files.isEmpty()) {
                var action = channel.sendFiles(files);
                if (message != null && !message.isBlank()) {
                    action = action.setContent(message);
                }
                if (!embeds.isEmpty()) {
                    action.setEmbeds(embeds);
                }
                if (!components.isEmpty()) {
                    action.setComponents(components);
                }
                sentMessage = action.complete();
            } else if (!embeds.isEmpty() || !components.isEmpty()) {
                if (message != null && !message.isBlank()) {
                    sentMessage = channel.sendMessage(message)
                            .setEmbeds(embeds)
                            .setComponents(components)
                            .complete();
                } else if (!embeds.isEmpty()) {
                    sentMessage = channel.sendMessageEmbeds(embeds)
                            .setComponents(components)
                            .complete();
                } else {
                    sentMessage = channel.sendMessageEmbeds(java.util.Collections.emptyList())
                            .setComponents(components)
                            .complete();
                }
            } else {
                if (message == null || message.isBlank()) {
                    throw new IllegalArgumentException("message, embedsJson or componentsJson is required");
                }
                sentMessage = channel.sendMessage(message).complete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
        }

        return "Message sent successfully. Message link: " + sentMessage.getJumpUrl();
    }

    /**
     * Edits an existing message in a specified Discord channel.
     * Supports updating content, embeds (e.g. progress bars), and components (add/remove buttons).
     *
     * @param channelId  The ID of the channel containing the message.
     * @param messageId  The ID of the message to be edited.
     * @param newMessage Optional new text content.
     * @param embedsJson Optional new embeds JSON (replaces existing embeds).
     * @param componentsJson Optional new components JSON (replaces buttons/selects).
     * @return A confirmation message with a link to the edited message.
     */
    @Tool(name = "edit_message", description = "Edit a message (update text/embed/components). Perfect for progress updates or dynamic buttons.")
    public String editMessage(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Specific message ID") String messageId,
                              @ToolParam(description = "New message content (optional)", required = false) String newMessage,
                              @ToolParam(description = "JSON array of embeds to replace with (optional)", required = false) String embedsJson,
                              @ToolParam(description = "JSON array of ActionRow components to replace with (optional)", required = false) String componentsJson) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message messageById = channel.retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        List<MessageEmbed> embeds = parseEmbeds(embedsJson);
        List<ActionRow> components = parseComponents(componentsJson);

        Message editedMessage;
        try {
            var editAction = messageById.editMessage(newMessage != null ? newMessage : "");
            if (!embeds.isEmpty()) {
                editAction.setEmbeds(embeds);
            }
            if (!components.isEmpty()) {
                editAction.setComponents(components);
            }
            // If both content empty and no embeds/components change, still allow
            editedMessage = editAction.complete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to edit message: " + e.getMessage(), e);
        }

        return "Message edited successfully. Message link: " + editedMessage.getJumpUrl();
    }

    /**
     * Deletes a message from a specified Discord channel.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to be deleted.
     * @return A confirmation message indicating the message was deleted successfully.
     */
    @Tool(name = "delete_message", description = "Delete a message from a specific channel")
    public String deleteMessage(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Specific message ID") String messageId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message messageById = channel.retrieveMessageById(messageId).complete();
        if (messageById == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        messageById.delete().queue();
        return "Message deleted successfully";
    }

    /**
     * Reads message history from a specified Discord channel.
     *
     * @param channelId The ID of the channel from which to read messages.
     * @param count     Optional number of messages to retrieve (default is 100, max is 100).
     * @param before    Optional message ID to fetch messages before this message.
     * @param after     Optional message ID to fetch messages after this message.
     * @param around    Optional message ID to fetch messages around this message.
     * @return A formatted string containing the retrieved messages.
     */
    @Tool(name = "read_messages", description = "Read message history from a specific channel, optionally paginated with before/after/around")
    public String readMessages(@ToolParam(description = "Discord channel ID") String channelId,
                               @ToolParam(description = "Number of messages to retrieve (1-100)", required = false) String count,
                               @ToolParam(description = "Message ID to fetch messages before this message", required = false) String before,
                               @ToolParam(description = "Message ID to fetch messages after this message", required = false) String after,
                               @ToolParam(description = "Message ID to fetch messages around this message", required = false) String around) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        int limit = parseMessageLimit(count);
        validateCursorParameters(before, after, around);

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        List<Message> messages;
        if (isProvided(before)) {
            messages = channel.getHistoryBefore(before, limit).complete().getRetrievedHistory();
        } else if (isProvided(after)) {
            messages = channel.getHistoryAfter(after, limit).complete().getRetrievedHistory();
        } else if (isProvided(around)) {
            messages = channel.getHistoryAround(around, limit).complete().getRetrievedHistory();
        } else {
            messages = channel.getHistory().retrievePast(limit).complete();
        }
        List<String> formatedMessages = formatMessages(messages);
        return "**Retrieved " + messages.size() + " messages:** \n" + String.join("\n", formatedMessages);
    }

    private int parseMessageLimit(String count) {
        if (count == null || count.isBlank()) {
            return 100;
        }

        int limit;
        try {
            limit = Integer.parseInt(count);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("count must be an integer between 1 and 100");
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("count must be between 1 and 100");
        }
        return limit;
    }

    private void validateCursorParameters(String before, String after, String around) {
        if (before != null && before.isBlank()) {
            throw new IllegalArgumentException("before cannot be blank");
        }
        if (after != null && after.isBlank()) {
            throw new IllegalArgumentException("after cannot be blank");
        }
        if (around != null && around.isBlank()) {
            throw new IllegalArgumentException("around cannot be blank");
        }

        int providedCursors = (isProvided(before) ? 1 : 0)
                + (isProvided(after) ? 1 : 0)
                + (isProvided(around) ? 1 : 0);
        if (providedCursors > 1) {
            throw new IllegalArgumentException("before, after, and around are mutually exclusive; provide only one");
        }
    }

    private boolean isProvided(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Adds a reaction (emoji) to a specific message in a Discord channel.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message to which the reaction will be added.
     * @param emoji     The emoji to add as a reaction (can be a Unicode character or a custom emoji string).
     * @return A confirmation message with a link to the message that was reacted to.
     */
    @Tool(name = "add_reaction", description = "Add a reaction (emoji) to a specific message")
    public String addReaction(@ToolParam(description = "Discord channel ID") String channelId,
                              @ToolParam(description = "Discord message ID") String messageId,
                              @ToolParam(description = "Emoji (Unicode or string)") String emoji) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (emoji == null || emoji.isEmpty()) {
            throw new IllegalArgumentException("emoji cannot be null");
        }

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = channel.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        message.addReaction(Emoji.fromUnicode(emoji)).queue();
        return "Added reaction successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Removes a specified reaction (emoji) from a message in a Discord channel.
     *
     * @param channelId The ID of the channel containing the message.
     * @param messageId The ID of the message from which the reaction will be removed.
     * @param emoji     The emoji to remove from the message (can be a Unicode character or a custom emoji string).
     * @return A confirmation message with a link to the message.
     */
    @Tool(name = "remove_reaction", description = "Remove a specified reaction (emoji) from a message")
    public String removeReaction(@ToolParam(description = "Discord channel ID") String channelId,
                                 @ToolParam(description = "Discord message ID") String messageId,
                                 @ToolParam(description = "Emoji (Unicode or string)") String emoji) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (emoji == null || emoji.isEmpty()) {
            throw new IllegalArgumentException("emoji cannot be null");
        }

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = channel.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }
        message.removeReaction(Emoji.fromUnicode(emoji)).queue();
        return "Removed reaction successfully. Message link: " + message.getJumpUrl();
    }

    /**
     * Retrieves attachment metadata from a specific message in a Discord channel.
     *
     * @param channelId    The ID of the channel containing the message.
     * @param messageId    The ID of the message to retrieve attachments from.
     * @param attachmentId Optional ID of a specific attachment (if omitted, returns all).
     * @return A formatted string containing attachment metadata.
     */
    @Tool(name = "get_attachment", description = "Get attachment metadata (filename, size, content type, URLs) from a specific message. Returns info only, does not download files.")
    public String getAttachment(@ToolParam(description = "Discord channel ID") String channelId,
                                @ToolParam(description = "Discord message ID") String messageId,
                                @ToolParam(description = "Specific attachment ID (omit to get all attachments)", required = false) String attachmentId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        MessageChannel channel = getMessageChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found by channelId");
        }
        Message message = channel.retrieveMessageById(messageId).complete();
        if (message == null) {
            throw new IllegalArgumentException("Message not found by messageId");
        }

        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.isEmpty()) {
            return "This message has no attachments.";
        }

        if (attachmentId != null && !attachmentId.isEmpty()) {
            Message.Attachment attachment = attachments.stream()
                    .filter(a -> a.getId().equals(attachmentId))
                    .findFirst()
                    .orElse(null);
            if (attachment == null) {
                throw new IllegalArgumentException("Attachment not found by attachmentId");
            }
            return formatAttachmentDetail(attachment);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Found ").append(attachments.size()).append(" attachment(s):**\n");
        for (Message.Attachment attachment : attachments) {
            sb.append(formatAttachmentDetail(attachment)).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatAttachmentDetail(Message.Attachment attachment) {
        return String.format(
                "- %s\n  Proxy URL: %s",
                formatAttachmentSummary(attachment),
                attachment.getProxyUrl()
        );
    }

    private String formatAttachmentSummary(Message.Attachment attachment) {
        return String.format(
                "(Attachment ID: %s) `%s` (%s, %s) URL: %s",
                attachment.getId(),
                attachment.getFileName(),
                formatFileSize(attachment.getSize()),
                attachment.getContentType() != null ? attachment.getContentType() : "unknown",
                attachment.getUrl()
        );
    }

    private List<String> formatMessages(List<Message> messages) {
        return messages.stream()
                .map(m -> {
                    String authorName = m.getAuthor().getName();
                    String timestamp = m.getTimeCreated().toString();
                    String content = m.getContentDisplay();
                    String msgId = m.getId();

                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("- (ID: %s) **[%s]** `%s`: ```%s```", msgId, authorName, timestamp, content));

                    List<Message.Attachment> attachments = m.getAttachments();
                    if (!attachments.isEmpty()) {
                        sb.append("\n  Attachments:");
                        for (Message.Attachment attachment : attachments) {
                            sb.append("\n    - ").append(formatAttachmentSummary(attachment));
                        }
                    }

                    return sb.toString();
                }).toList();
    }

    private String formatFileSize(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== Rich Embed / Component / File Parsers (used by send/edit + InteractionService) ====================

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse embedsJson into JDA MessageEmbed list. Supports full spec.
     */
    public static List<MessageEmbed> parseEmbeds(String embedsJson) {
        List<MessageEmbed> result = new ArrayList<>();
        if (embedsJson == null || embedsJson.isBlank()) return result;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(embedsJson);
            if (!arr.isArray()) {
                // allow single object too
                arr = mapper.createArrayNode().add(arr);
            }
            for (JsonNode e : arr) {
                EmbedBuilder b = new EmbedBuilder();
                if (e.hasNonNull("title")) b.setTitle(e.get("title").asText());
                if (e.hasNonNull("description")) b.setDescription(e.get("description").asText());
                if (e.hasNonNull("url")) b.setUrl(e.get("url").asText());
                if (e.hasNonNull("color")) {
                    int color = e.get("color").isInt() ? e.get("color").asInt() : Integer.parseInt(e.get("color").asText().replace("#", ""), 16);
                    b.setColor(color);
                }
                if (e.hasNonNull("timestamp")) {
                    try { b.setTimestamp(Instant.parse(e.get("timestamp").asText())); } catch (Exception ignored) {}
                }

                // author
                if (e.has("author") && e.get("author").isObject()) {
                    JsonNode a = e.get("author");
                    String name = a.path("name").asText(null);
                    String url = a.path("url").asText(null);
                    String icon = a.path("icon_url").asText(null);
                    if (name != null) b.setAuthor(name, url, icon);
                }
                // thumbnail
                if (e.has("thumbnail") && e.get("thumbnail").hasNonNull("url")) {
                    b.setThumbnail(e.get("thumbnail").get("url").asText());
                }
                // image
                if (e.has("image") && e.get("image").hasNonNull("url")) {
                    b.setImage(e.get("image").get("url").asText());
                }
                // footer
                if (e.has("footer") && e.get("footer").isObject()) {
                    JsonNode f = e.get("footer");
                    b.setFooter(f.path("text").asText(null), f.path("icon_url").asText(null));
                }
                // fields
                if (e.has("fields") && e.get("fields").isArray()) {
                    for (JsonNode f : e.get("fields")) {
                        String name = f.path("name").asText("");
                        String value = f.path("value").asText("");
                        boolean inline = f.path("inline").asBoolean(false);
                        if (!name.isBlank() && !value.isBlank()) {
                            b.addField(name, value, inline);
                        }
                    }
                }
                result.add(b.build());
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid embedsJson: " + ex.getMessage());
        }
        return result;
    }

    /**
     * Parse componentsJson (ActionRows) into List<ActionRow>.
     * Supports buttons and string select menus (primary for most bots).
     */
    public static List<ActionRow> parseComponents(String componentsJson) {
        List<ActionRow> rows = new ArrayList<>();
        if (componentsJson == null || componentsJson.isBlank()) return rows;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(componentsJson);
            if (!arr.isArray()) arr = mapper.createArrayNode().add(arr);

            for (JsonNode rowNode : arr) {
                JsonNode compsNode = rowNode.has("components") ? rowNode.get("components") : rowNode;
                if (!compsNode.isArray()) continue;

                List<net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent> comps = new ArrayList<>();

                for (JsonNode c : compsNode) {
                    int type = c.path("type").asInt(0);
                    if (type == 2) { // Button
                        int styleInt = c.path("style").asInt(1);
                        ButtonStyle style = switch (styleInt) {
                            case 1 -> ButtonStyle.PRIMARY;
                            case 2 -> ButtonStyle.SECONDARY;
                            case 3 -> ButtonStyle.SUCCESS;
                            case 4 -> ButtonStyle.DANGER;
                            case 5 -> ButtonStyle.LINK;
                            default -> ButtonStyle.PRIMARY;
                        };
                        String label = c.path("label").asText(null);
                        String customId = c.path("custom_id").asText(null); // IMPORTANT: snake_case as per docs
                        if (style == ButtonStyle.LINK) {
                            String url = c.path("url").asText(null);
                            if (url == null) url = c.path("url").asText("");
                            Button btn = Button.link(url, label != null ? label : "Link");
                            if (c.hasNonNull("emoji")) btn = btn.withEmoji(parseEmoji(c.get("emoji")));
                            if (c.path("disabled").asBoolean(false)) btn = btn.asDisabled();
                            comps.add(btn);
                        } else {
                            if (customId == null || customId.isBlank()) {
                                throw new IllegalArgumentException("Button requires custom_id (use snake_case)");
                            }
                            Button btn = Button.of(style, customId, label != null ? label : "Button");
                            if (c.hasNonNull("emoji")) btn = btn.withEmoji(parseEmoji(c.get("emoji")));
                            if (c.path("disabled").asBoolean(false)) btn = btn.asDisabled();
                            comps.add(btn);
                        }
                    } else if (type == 3) { // String Select
                        String customId = c.path("custom_id").asText(null);
                        if (customId == null || customId.isBlank()) {
                            throw new IllegalArgumentException("SelectMenu requires custom_id");
                        }
                        String placeholder = c.path("placeholder").asText("Select...");
                        int min = c.path("min_values").asInt(1);
                        int max = c.path("max_values").asInt(1);

                        List<SelectOption> options = new ArrayList<>();
                        if (c.has("options") && c.get("options").isArray()) {
                            for (JsonNode opt : c.get("options")) {
                                String val = opt.path("value").asText("");
                                String lbl = opt.path("label").asText(val);
                                String desc = opt.path("description").asText(null);
                                SelectOption so = (desc != null && !desc.isBlank())
                                    ? SelectOption.of(lbl, val).withDescription(desc)
                                    : SelectOption.of(lbl, val);
                                if (opt.hasNonNull("emoji")) so = so.withEmoji(parseEmoji(opt.get("emoji")));
                                if (opt.path("default").asBoolean(false)) so = so.withDefault(true);
                                options.add(so);
                            }
                        }
                        if (options.isEmpty()) continue;

                        StringSelectMenu.Builder menu = StringSelectMenu.create(customId)
                                .setPlaceholder(placeholder)
                                .setMinValues(Math.max(0, min))
                                .setMaxValues(Math.max(1, max));
                        options.forEach(menu::addOptions);
                        comps.add(menu.build());
                    }
                    // type 4= text input is for modals only
                }

                if (!comps.isEmpty()) {
                    rows.add(ActionRow.of(comps));  // Collection overload
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid componentsJson: " + ex.getMessage());
        }
        return rows;
    }

    private static net.dv8tion.jda.api.entities.emoji.Emoji parseEmoji(JsonNode emojiNode) {
        if (emojiNode == null) return null;
        if (emojiNode.hasNonNull("name")) {
            String name = emojiNode.get("name").asText();
            // custom emoji id support if id present
            if (emojiNode.hasNonNull("id")) {
                String id = emojiNode.get("id").asText();
                boolean animated = emojiNode.path("animated").asBoolean(false);
                return net.dv8tion.jda.api.entities.emoji.Emoji.fromCustom(name, Long.parseLong(id), animated);
            }
            return net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(name);
        }
        return net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(emojiNode.asText());
    }

    /**
     * Parse filesJson. Prefers local "path". Falls back to base64 or url.
     */
    public static List<FileUpload> parseFiles(String filesJson) {
        List<FileUpload> uploads = new ArrayList<>();
        if (filesJson == null || filesJson.isBlank()) return uploads;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(filesJson);
            if (!arr.isArray()) arr = mapper.createArrayNode().add(arr);

            for (JsonNode f : arr) {
                String filename = f.path("filename").asText(null);
                if (f.hasNonNull("path")) {
                    Path p = Paths.get(f.get("path").asText());
                    if (!Files.exists(p)) {
                        throw new IllegalArgumentException("File not found at path: " + p);
                    }
                    byte[] bytes = Files.readAllBytes(p);
                    String useName = (filename != null && !filename.isBlank()) ? filename : p.getFileName().toString();
                    uploads.add(FileUpload.fromData(bytes, useName));
                } else if (f.hasNonNull("base64")) {
                    String b64 = f.get("base64").asText();
                    if (b64.startsWith("data:")) b64 = b64.substring(b64.indexOf(",") + 1);
                    byte[] bytes = Base64.getDecoder().decode(b64);
                    String name = filename != null ? filename : "file.bin";
                    uploads.add(FileUpload.fromData(bytes, name));
                } else if (f.hasNonNull("url")) {
                    String url = f.get("url").asText();
                    byte[] bytes = URI.create(url).toURL().openStream().readAllBytes();
                    String name = filename != null ? filename : url.substring(url.lastIndexOf('/') + 1);
                    uploads.add(FileUpload.fromData(bytes, name));
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid filesJson or file error: " + ex.getMessage());
        }
        return uploads;
    }
}

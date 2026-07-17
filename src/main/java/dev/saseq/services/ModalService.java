package dev.saseq.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.label.Label;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModalService {

    private final JDA jda;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModalService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Creates a modal payload that can be used with Discord's interaction response endpoint.
     * Modals require ActionRows containing TextInputs, but JDA 6.x requires special handling:
     * - TextInput extends LabelChildComponent (not ModalTopLevelComponent)
     * - Modal.Builder.addComponents() accepts ModalTopLevelComponent (ActionRow, Label, TextDisplay)
     * - To include TextInput in modal, wrap in Label component or use raw JSON payload
     *
     * @param customId       The custom ID of the modal.
     * @param title          The title of the modal.
     * @param componentsJson JSON array of ActionRow components with TextInputs.
     * @return JSON string of the modal payload.
     */
    @Tool(name = "create_modal_payload", description = "Create a modal payload JSON. IMPORTANT: Call with FLAT parameters: customId (string), title (string), componentsJson (string). Do NOT nest under 'data' or use snake_case for top-level params.")
    public String createModalPayload(@ToolParam(description = "Custom ID for the modal (use camelCase: customId, not custom_id for the tool parameter itself)") String customId,
                                     @ToolParam(description = "Modal title") String title,
                                     @ToolParam(description = "JSON array of ActionRow components with TextInputs (use custom_id INSIDE the JSON for each input)") String componentsJson) {
        if (customId == null || customId.isEmpty()) {
            throw new IllegalArgumentException("customId cannot be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title cannot be null");
        }
        if (componentsJson == null || componentsJson.isEmpty()) {
            throw new IllegalArgumentException("componentsJson cannot be null");
        }

        List<ModalTopLevelComponent> components = parseModalComponents(componentsJson);
        if (components.isEmpty()) {
            throw new IllegalArgumentException("At least one TextInput component is required");
        }

        Modal modal = Modal.create(customId, title)
                .addComponents(components.toArray(new ModalTopLevelComponent[0]))
                .build();

        try {
            return objectMapper.writeValueAsString(modal);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize modal: " + e.getMessage());
        }
    }

    /**
     * Sends a modal in response to an interaction (slash command, button, select menu).
     * Note: This requires the interaction context. For webhook-based interactions,
     * use create_modal_payload and send via Discord interaction response endpoint.
     *
     * @param interactionId   The ID of the interaction to respond to.
     * @param interactionToken The token of the interaction to respond to.
     * @param customId        The custom ID of the modal.
     * @param title           The title of the modal.
     * @param componentsJson  JSON array of ActionRow components with TextInputs.
     * @return A confirmation message with the modal payload JSON.
     */
    @Tool(name = "send_modal", description = "Send a modal in response to an interaction. Requires a live interactionId + interactionToken from a pending button/select/slash (from list_pending_interactions). Use create_modal_payload for JSON only.")
    public String sendModal(@ToolParam(description = "Discord interaction ID (from pending interaction)") String interactionId,
                            @ToolParam(description = "Discord interaction token (from pending interaction)") String interactionToken,
                            @ToolParam(description = "Custom ID for the modal") String customId,
                            @ToolParam(description = "Modal title") String title,
                            @ToolParam(description = "JSON array of ActionRow components with TextInputs") String componentsJson) {
        if (interactionId == null || interactionId.isEmpty()) {
            throw new IllegalArgumentException("interactionId cannot be null");
        }
        if (interactionToken == null || interactionToken.isEmpty()) {
            throw new IllegalArgumentException("interactionToken cannot be null");
        }
        if (customId == null || customId.isEmpty()) {
            throw new IllegalArgumentException("customId cannot be null");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title cannot be null");
        }
        if (componentsJson == null || componentsJson.isEmpty()) {
            throw new IllegalArgumentException("componentsJson cannot be null");
        }

        String modalPayload = createModalPayload(customId, title, componentsJson);

        // In practice, you would POST to: https://discord.com/api/v10/interactions/{id}/{token}/callback
        // with body: {"type": 9, "data": <modalPayload>}
        return "Modal payload created. To send it, POST to Discord interaction endpoint:\n" +
                "POST https://discord.com/api/v10/interactions/" + interactionId + "/" + interactionToken + "/callback\n" +
                "Content-Type: application/json\n" +
                "Body: {\"type\": 9, \"data\": " + modalPayload + "}";
    }

    /**
     * Responds to a modal submit interaction.
     *
     * @param interactionId   The ID of the interaction to respond to.
     * @param interactionToken The token of the interaction to respond to.
     * @param content         Optional response message content.
     * @param ephemeral       Whether the response should be ephemeral (only visible to the user). Default: false.
     * @return A confirmation message.
     */
    @Tool(name = "respond_modal", description = "Respond to a modal submit interaction. Requires interactionId + interactionToken from the pending MODAL_SUBMIT event.")
    public String respondModal(@ToolParam(description = "Discord interaction ID (from pending modal submit)") String interactionId,
                               @ToolParam(description = "Discord interaction token (from pending modal submit)") String interactionToken,
                               @ToolParam(description = "Response message content", required = false) String content,
                               @ToolParam(description = "Whether response is ephemeral (true/false)", required = false) String ephemeral) {
        if (interactionId == null || interactionId.isEmpty()) {
            throw new IllegalArgumentException("interactionId cannot be null");
        }
        if (interactionToken == null || interactionToken.isEmpty()) {
            throw new IllegalArgumentException("interactionToken cannot be null");
        }

        boolean isEphemeral = false;
        if (ephemeral != null && !ephemeral.isEmpty()) {
            isEphemeral = Boolean.parseBoolean(ephemeral);
        }

        // POST to: https://discord.com/api/v10/interactions/{id}/{token}/callback
        // Type 4 = Channel Message with Source, flags = 64 for ephemeral
        return "Modal response prepared. To send it, POST to Discord interaction endpoint:\n" +
                "POST https://discord.com/api/v10/interactions/" + interactionId + "/" + interactionToken + "/callback\n" +
                "Content-Type: application/json\n" +
                "Body: {\"type\": 4, \"data\": {\"content\": " + (content != null ? "\"" + content.replace("\"", "\\\"") + "\"" : "null") + ", \"flags\": " + (isEphemeral ? "64" : "0") + "}}";
    }

    private List<ModalTopLevelComponent> parseModalComponents(String componentsJson) {
        List<ModalTopLevelComponent> components = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(componentsJson);
            if (!array.isArray()) {
                throw new IllegalArgumentException("componentsJson must be a JSON array of ActionRows");
            }
            for (JsonNode rowNode : array) {
                JsonNode comps = rowNode.has("components") ? rowNode.get("components") : rowNode;
                if (!comps.isArray()) {
                    throw new IllegalArgumentException("Each ActionRow must have a 'components' array");
                }

                // For JDA 6.x, TextInput cannot be directly added to Modal.
                // Option 1: Wrap each TextInput in a Label component (Label implements ModalTopLevelComponent)
                // Option 2: Use ActionRow for buttons/selects only, TextInput must be in Labels
                // We'll use Label wrapper for TextInputs
                List<ModalTopLevelComponent> rowComponents = new ArrayList<>();
                for (JsonNode comp : comps) {
                    int type = comp.has("type") ? comp.get("type").asInt() : 0;
                    if (type == 4) { // Text Input (type 4)
                        TextInput textInput = parseTextInput(comp);
                        if (textInput != null) {
                            // Store label during parsing
                            String labelText = comp.has("label") && !comp.get("label").isNull() ? comp.get("label").asText() : "";
                            // Wrap TextInput in Label to make it ModalTopLevelComponent compatible
                            Label label = Label.of(labelText, textInput);
                            rowComponents.add(label);
                        }
                    } else {
                        // Other component types (Button=2, SelectMenu=3) can use ActionRow
                        // For now, skip non-TextInput in modals
                        throw new IllegalArgumentException("Modals only support TextInput components (type 4). Other types must be sent as regular message components.");
                    }
                }
                if (!rowComponents.isEmpty()) {
                    // If multiple components in one row, create separate Labels for each
                    components.addAll(rowComponents);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid componentsJson: " + e.getMessage());
        }
        return components;
    }

    private TextInput parseTextInput(JsonNode node) {
        String customId = node.has("custom_id") && !node.get("custom_id").isNull() ? node.get("custom_id").asText() : null;
        if (customId == null || customId.isEmpty()) {
            throw new IllegalArgumentException("TextInput requires 'custom_id'");
        }
        String label = node.has("label") && !node.get("label").isNull() ? node.get("label").asText() : "";
        int styleValue = node.has("style") ? node.get("style").asInt() : 1;
        TextInputStyle style = switch (styleValue) {
            case 1 -> TextInputStyle.SHORT;
            case 2 -> TextInputStyle.PARAGRAPH;
            default -> TextInputStyle.SHORT;
        };
        String placeholder = node.has("placeholder") && !node.get("placeholder").isNull() ? node.get("placeholder").asText() : null;
        String value = node.has("value") && !node.get("value").isNull() ? node.get("value").asText() : null;
        int minLength = node.has("min_length") ? node.get("min_length").asInt() : 0;
        int maxLength = node.has("max_length") ? node.get("max_length").asInt() : 4000;
        boolean required = node.has("required") && node.get("required").asBoolean();

        TextInput.Builder builder = TextInput.create(customId, style)
                .setMinLength(minLength)
                .setMaxLength(maxLength)
                .setRequired(required);

        if (placeholder != null) builder.setPlaceholder(placeholder);
        if (value != null) builder.setValue(value);

        return builder.build();
    }
}
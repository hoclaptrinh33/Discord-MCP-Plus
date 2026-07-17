package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.messages.MessagePoll;
import net.dv8tion.jda.api.utils.messages.MessagePollBuilder;
import net.dv8tion.jda.api.utils.messages.MessagePollData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for Discord Native Polls (using official Discord Poll API via JDA).
 * This replaces the old custom embed + button implementation.
 */
@Service
public class PollService {

    private static final Logger log = LoggerFactory.getLogger(PollService.class);

    private final JDA jda;

    // Store metadata for created polls (messageId -> PollMeta)
    private final Map<String, PollMeta> activePolls = new ConcurrentHashMap<>();

    public static class PollMeta {
        public String id;           // internal short id or messageId
        public String question;
        public List<String> options;
        public String messageId;
        public String channelId;
        public long expiresAt;
        public boolean ended = false;
    }

    public PollService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Creates a native Discord Poll.
     */
    @Tool(name = "create_poll", description = "Create a native Discord poll (uses official Discord poll UI). Returns the message link.")
    public String createPoll(
            @ToolParam(description = "Channel ID to post the poll") String channelId,
            @ToolParam(description = "Poll question") String question,
            @ToolParam(description = "List of poll options (max 10)") List<String> options,
            @ToolParam(description = "Duration in minutes (1-10080)", required = false) String durationMinutesStr) {

        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId is required");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        if (options == null || options.isEmpty() || options.size() > 10) {
            throw new IllegalArgumentException("options must be 1-10 items");
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            // try thread
            var thread = jda.getThreadChannelById(channelId);
            if (thread == null) throw new IllegalArgumentException("Channel not found: " + channelId);
            // Threads can host polls too in some cases, but use parent if needed
            channel = thread.getParentChannel() instanceof TextChannel ? (TextChannel) thread.getParentChannel() : null;
            if (channel == null) throw new IllegalArgumentException("Cannot post poll in this channel type");
        }

        int durationMinutes = 60; // default 1 hour
        if (durationMinutesStr != null && !durationMinutesStr.isBlank()) {
            try { durationMinutes = Integer.parseInt(durationMinutesStr); } catch (NumberFormatException ignored) {}
            if (durationMinutes < 1) durationMinutes = 1;
            if (durationMinutes > 10080) durationMinutes = 10080;
        }

        MessagePollBuilder builder = new MessagePollBuilder(question);
        builder.setDuration(Duration.ofMinutes(durationMinutes));
        for (String option : options) {
            builder.addAnswer(option.trim());
        }
        MessagePollData pollData = builder.build();

        Message sent = channel.sendMessage(" ") // content can be empty for pure poll
                .setPoll(pollData)
                .complete();

        String pollId = sent.getId(); // use message ID as primary key
        PollMeta meta = new PollMeta();
        meta.id = pollId;
        meta.question = question;
        meta.options = new ArrayList<>(options);
        meta.messageId = sent.getId();
        meta.channelId = channelId;
        meta.expiresAt = System.currentTimeMillis() + (durationMinutes * 60L * 1000L);

        activePolls.put(pollId, meta);

        log.info("Created native Discord poll {} in channel {} (message {})", pollId, channelId, sent.getId());
        return "Native poll created successfully. Message link: " + sent.getJumpUrl() + " | Poll ID: " + pollId;
    }

    /**
     * Gets results of a native poll.
     */
    @Tool(name = "get_poll_results", description = "Fetch current results of a native Discord poll by message ID.")
    public String getPollResults(@ToolParam(description = "Poll message ID (or poll ID)") String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId is required");
        }

        Message msg = null;
        PollMeta meta = activePolls.get(messageId);
        String chId = meta != null ? meta.channelId : null;

        // Try to find the message
        if (chId != null) {
            TextChannel ch = jda.getTextChannelById(chId);
            if (ch != null) {
                try { msg = ch.retrieveMessageById(messageId).complete(); } catch (Exception ignored) {}
            }
        }
        if (msg == null) {
            // fallback search in all text channels (slow but works for small guilds)
            for (TextChannel ch : jda.getTextChannels()) {
                try {
                    msg = ch.retrieveMessageById(messageId).complete();
                    if (msg != null) break;
                } catch (Exception ignored) {}
            }
        }

        if (msg == null) {
            return "Poll message not found: " + messageId;
        }

        MessagePoll poll = msg.getPoll();
        if (poll == null) {
            return "This message does not contain a poll.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Poll:** ").append(poll.getQuestion().getText()).append("\n");
        sb.append("Status: ").append(poll.isFinalizedVotes() || poll.isExpired() ? "Ended" : "Active").append("\n\n");

        List<MessagePoll.Answer> answers = poll.getAnswers();
        long totalVotes = 0;
        for (MessagePoll.Answer ans : answers) {
            totalVotes += ans.getVotes();
        }

        for (int i = 0; i < answers.size(); i++) {
            MessagePoll.Answer ans = answers.get(i);
            long count = ans.getVotes();
            double percent = totalVotes > 0 ? (count * 100.0 / totalVotes) : 0;
            sb.append(String.format("%d. **%s** — %d votes (%.1f%%)\n",
                    i + 1, ans.getText(), count, percent));
        }

        sb.append("\nTotal votes: ").append(totalVotes);
        if (meta != null) {
            sb.append(" | Expires: ").append(new java.util.Date(meta.expiresAt));
        }

        return sb.toString();
    }

    /**
     * Ends a poll early.
     */
    @Tool(name = "end_poll", description = "End a native Discord poll early (by message ID).")
    public String endPoll(@ToolParam(description = "Poll message ID") String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId is required");
        }

        PollMeta meta = activePolls.get(messageId);
        if (meta == null) {
            // try to find the message anyway
        }

        TextChannel channel = null;
        if (meta != null) {
            channel = jda.getTextChannelById(meta.channelId);
        }

        if (channel == null) {
            // brute force
            for (TextChannel ch : jda.getTextChannels()) {
                try {
                    Message m = ch.retrieveMessageById(messageId).complete();
                    if (m != null && m.getPoll() != null) {
                        channel = ch;
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (channel == null) {
            return "Could not locate poll channel for message " + messageId;
        }

        try {
            Message msg = channel.retrieveMessageById(messageId).complete();
            if (msg.getPoll() != null) {
                msg.endPoll().complete();  // ends the poll
                if (meta != null) meta.ended = true;
                log.info("Ended poll {}", messageId);
                return "Poll ended successfully. Message: " + msg.getJumpUrl();
            }
            return "Message does not have an active poll.";
        } catch (Exception e) {
            return "Failed to end poll: " + e.getMessage();
        }
    }

    // Internal method used by demo / legacy callers (kept for compatibility)
    public String createPoll(TextChannel channel, String question, List<String> options, int durationMinutes) {
        // delegate to the rich version
        return createPoll(channel.getId(), question, options, String.valueOf(durationMinutes));
    }

    public List<PollMeta> getActivePolls() {
        return new ArrayList<>(activePolls.values());
    }
}

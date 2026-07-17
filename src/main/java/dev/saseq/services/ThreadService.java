package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThreadService {

    private final JDA jda;

    public ThreadService(JDA jda) {
        this.jda = jda;
    }

    private IThreadContainer getThreadContainer(String channelId) {
        TextChannel textChannel = jda.getTextChannelById(channelId);
        if (textChannel != null) {
            return textChannel;
        }
        // NewsChannel also implements IThreadContainer
        net.dv8tion.jda.api.entities.channel.concrete.NewsChannel newsChannel = jda.getNewsChannelById(channelId);
        if (newsChannel != null) {
            return newsChannel;
        }
        return null;
    }

    private ThreadChannel.AutoArchiveDuration parseAutoArchiveDuration(String autoArchiveDuration) {
        int minutes = 1440; // default 24 hours
        if (autoArchiveDuration != null && !autoArchiveDuration.isEmpty()) {
            try {
                minutes = Integer.parseInt(autoArchiveDuration);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid autoArchiveDuration: must be a number");
            }
        }
        return switch (minutes) {
            case 60 -> ThreadChannel.AutoArchiveDuration.TIME_1_HOUR;
            case 1440 -> ThreadChannel.AutoArchiveDuration.TIME_24_HOURS;
            case 4320 -> ThreadChannel.AutoArchiveDuration.TIME_3_DAYS;
            case 10080 -> ThreadChannel.AutoArchiveDuration.TIME_1_WEEK;
            default -> throw new IllegalArgumentException("Invalid autoArchiveDuration: must be 60, 1440, 4320, or 10080");
        };
    }

    /**
     * Creates a new public thread in a text channel.
     *
     * @param channelId The ID of the text channel to create the thread in.
     * @param name      The name of the thread.
     * @param messageId Optional message ID to create the thread from (creates a thread from an existing message).
     * @param autoArchiveDuration Auto-archive duration in minutes (60, 1440, 4320, 10080). Default: 1440 (24 hours).
     * @param reason    Optional reason for audit log.
     * @return The created thread's information.
     */
    @Tool(name = "create_thread", description = "Create a new public thread in a text channel")
    public String createThread(@ToolParam(description = "Channel ID to create thread in") String channelId,
                               @ToolParam(description = "Thread name") String name,
                               @ToolParam(description = "Optional message ID to create thread from", required = false) String messageId,
                               @ToolParam(description = "Auto-archive duration in minutes (60, 1440, 4320, 10080)", required = false) String autoArchiveDuration,
                               @ToolParam(description = "Reason for audit log", required = false) String reason) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        IThreadContainer container = getThreadContainer(channelId);
        if (container == null) {
            throw new IllegalArgumentException("Channel not found or does not support threads: " + channelId);
        }

        ThreadChannel.AutoArchiveDuration archiveDuration = parseAutoArchiveDuration(autoArchiveDuration);

        ThreadChannel thread;
        if (messageId != null && !messageId.isEmpty()) {
            // Create thread from existing message - use createThreadChannel with messageId
            thread = container.createThreadChannel(messageId)
                    .setName(name)
                    .setAutoArchiveDuration(archiveDuration)
                    .reason(reason)
                    .complete();
        } else {
            // Create new thread without message
            thread = container.createThreadChannel(name)
                    .setAutoArchiveDuration(archiveDuration)
                    .reason(reason)
                    .complete();
        }

        return String.format("Thread created: %s (ID: %s) in channel %s",
                thread.getName(), thread.getId(), channelId);
    }

    /**
     * Creates a new private thread in a text channel.
     *
     * @param channelId The ID of the text channel to create the thread in.
     * @param name      The name of the thread.
     * @param autoArchiveDuration Auto-archive duration in minutes (60, 1440, 4320, 10080). Default: 1440 (24 hours).
     * @param invitable Whether the thread is invitable. Default: true.
     * @param reason    Optional reason for audit log.
     * @return The created thread's information.
     */
    @Tool(name = "create_private_thread", description = "Create a new private thread in a text channel")
    public String createPrivateThread(@ToolParam(description = "Channel ID to create thread in") String channelId,
                                      @ToolParam(description = "Thread name") String name,
                                      @ToolParam(description = "Auto-archive duration in minutes (60, 1440, 4320, 10080)", required = false) String autoArchiveDuration,
                                      @ToolParam(description = "Whether thread is invitable (true/false)", required = false) String invitable,
                                      @ToolParam(description = "Reason for audit log", required = false) String reason) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null");
        }

        IThreadContainer container = getThreadContainer(channelId);
        if (container == null) {
            throw new IllegalArgumentException("Channel not found or does not support threads: " + channelId);
        }

        ThreadChannel.AutoArchiveDuration archiveDuration = parseAutoArchiveDuration(autoArchiveDuration);

        boolean isInvitable = true;
        if (invitable != null && !invitable.isEmpty()) {
            isInvitable = Boolean.parseBoolean(invitable);
        }

        ThreadChannel thread = container.createThreadChannel(name, true) // true = private thread
                .setAutoArchiveDuration(archiveDuration)
                .setInvitable(isInvitable)
                .reason(reason)
                .complete();

        return String.format("Private thread created: %s (ID: %s) in channel %s",
                thread.getName(), thread.getId(), channelId);
    }

    /**
     * Gets information about a thread.
     *
     * @param threadId The ID of the thread.
     * @return Thread information.
     */
    @Tool(name = "get_thread", description = "Get information about a thread")
    public String getThread(@ToolParam(description = "Thread ID") String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found: " + threadId);
        }

        String parentName = "Unknown";
        if (thread.getParentChannel() != null) {
            parentName = thread.getParentChannel().getName();
        }

        return String.format(
                "Thread: %s (ID: %s)\n" +
                "Type: %s\n" +
                "Parent Channel: %s\n" +
                "Owner ID: %s\n" +
                "Archived: %s\n" +
                "Locked: %s\n" +
                "Auto-archive Duration: %d minutes\n" +
                "Member Count: %d\n" +
                "Message Count: %d\n" +
                "Created: %s",
                thread.getName(), thread.getId(),
                thread.getType(),
                parentName,
                thread.getOwnerId(),
                thread.isArchived(),
                thread.isLocked(),
                thread.getAutoArchiveDuration().getMinutes(),
                thread.getMemberCount(),
                thread.getMessageCount(),
                thread.getTimeCreated().toString()
        );
    }

    /**
     * Lists all active threads in a channel.
     *
     * @param channelId The ID of the channel.
     * @return List of active threads.
     */
    @Tool(name = "list_threads", description = "List all active threads in a channel")
    public String listThreads(@ToolParam(description = "Channel ID") String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("channelId cannot be null");
        }

        IThreadContainer container = getThreadContainer(channelId);
        if (container == null) {
            throw new IllegalArgumentException("Channel not found or does not support threads: " + channelId);
        }

        List<ThreadChannel> threads = container.getThreadChannels();

        if (threads.isEmpty()) {
            return "No active threads in channel: " + channelId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Active threads in channel ").append(channelId).append(":\n");
        for (ThreadChannel thread : threads) {
            sb.append(String.format("- %s (ID: %s) | Type: %s | Archived: %s | Members: %d | Messages: %d\n",
                    thread.getName(), thread.getId(), thread.getType(), thread.isArchived(), thread.getMemberCount(), thread.getMessageCount()));
        }
        return sb.toString();
    }

    /**
     * Lists all active threads in a guild.
     *
     * @param guildId The ID of the guild.
     * @return List of active threads.
     */
    @Tool(name = "list_guild_threads", description = "List all active threads in a guild")
    public String listGuildThreads(@ToolParam(description = "Guild ID") String guildId) {
        if (guildId == null || guildId.isEmpty()) {
            throw new IllegalArgumentException("guildId cannot be null");
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalArgumentException("Guild not found: " + guildId);
        }

        List<ThreadChannel> threads = guild.retrieveActiveThreads().complete();

        if (threads.isEmpty()) {
            return "No active threads in guild: " + guildId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Active threads in guild ").append(guild.getName()).append(":\n");
        for (ThreadChannel thread : threads) {
            String parentName = thread.getParentChannel() != null ? thread.getParentChannel().getName() : "Unknown";
            sb.append(String.format("- %s (ID: %s) | Parent: #%s | Type: %s | Archived: %s | Members: %d\n",
                    thread.getName(), thread.getId(), parentName, thread.getType(), thread.isArchived(), thread.getMemberCount()));
        }
        return sb.toString();
    }

    /**
     * Archives or unarchives a thread.
     *
     * @param threadId The ID of the thread.
     * @param archived true to archive, false to unarchive.
     * @param reason   Optional reason for audit log.
     * @return Confirmation message.
     */
    @Tool(name = "archive_thread", description = "Archive or unarchive a thread")
    public String archiveThread(@ToolParam(description = "Thread ID") String threadId,
                                @ToolParam(description = "true to archive, false to unarchive") String archived,
                                @ToolParam(description = "Reason for audit log", required = false) String reason) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (archived == null || archived.isEmpty()) {
            throw new IllegalArgumentException("archived cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found: " + threadId);
        }

        boolean shouldArchive = Boolean.parseBoolean(archived);

        ThreadChannelManager manager = thread.getManager();
        manager.setArchived(shouldArchive);
        if (reason != null && !reason.isEmpty()) {
            manager.reason(reason);
        }
        manager.complete();

        return String.format("Thread %s: %s (ID: %s)", shouldArchive ? "archived" : "unarchived", thread.getName(), thread.getId());
    }

    /**
     * Locks or unlocks a thread.
     *
     * @param threadId The ID of the thread.
     * @param locked   true to lock, false to unlock.
     * @param reason   Optional reason for audit log.
     * @return Confirmation message.
     */
    @Tool(name = "lock_thread", description = "Lock or unlock a thread")
    public String lockThread(@ToolParam(description = "Thread ID") String threadId,
                             @ToolParam(description = "true to lock, false to unlock") String locked,
                             @ToolParam(description = "Reason for audit log", required = false) String reason) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (locked == null || locked.isEmpty()) {
            throw new IllegalArgumentException("locked cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found: " + threadId);
        }

        boolean shouldLock = Boolean.parseBoolean(locked);

        ThreadChannelManager manager = thread.getManager();
        manager.setLocked(shouldLock);
        if (reason != null && !reason.isEmpty()) {
            manager.reason(reason);
        }
        manager.complete();

        return String.format("Thread %s: %s (ID: %s)", shouldLock ? "locked" : "unlocked", thread.getName(), thread.getId());
    }

    /**
     * Adds a user to a thread.
     *
     * @param threadId The ID of the thread.
     * @param userId   The ID of the user to add.
     * @return Confirmation message.
     */
    @Tool(name = "add_thread_member", description = "Add a user to a thread")
    public String addThreadMember(@ToolParam(description = "Thread ID") String threadId,
                                  @ToolParam(description = "User ID to add") String userId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found: " + threadId);
        }

        User user = jda.getUserById(userId);
        if (user == null) {
            // Try to get as member
            if (thread.getGuild() != null) {
                Member member = thread.getGuild().getMemberById(userId);
                if (member != null) {
                    thread.addThreadMember(member).complete();
                    return String.format("Added member %s to thread %s (ID: %s)", userId, thread.getName(), thread.getId());
                }
            }
            throw new IllegalArgumentException("User not found: " + userId);
        }

        thread.addThreadMember(user).complete();

        return String.format("Added user %s to thread %s (ID: %s)", userId, thread.getName(), thread.getId());
    }

    /**
     * Removes a user from a thread.
     *
     * @param threadId The ID of the thread.
     * @param userId   The ID of the user to remove.
     * @return Confirmation message.
     */
    @Tool(name = "remove_thread_member", description = "Remove a user from a thread")
    public String removeThreadMember(@ToolParam(description = "Thread ID") String threadId,
                                     @ToolParam(description = "User ID to remove") String userId) {
        if (threadId == null || threadId.isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null) {
            throw new IllegalArgumentException("Thread not found: " + threadId);
        }

        User user = jda.getUserById(userId);
        if (user == null) {
            if (thread.getGuild() != null) {
                Member member = thread.getGuild().getMemberById(userId);
                if (member != null) {
                    thread.removeThreadMember(member).complete();
                    return String.format("Removed member %s from thread %s (ID: %s)", userId, thread.getName(), thread.getId());
                }
            }
            throw new IllegalArgumentException("User not found: " + userId);
        }

        thread.removeThreadMember(user).complete();

        return String.format("Removed user %s from thread %s (ID: %s)", userId, thread.getName(), thread.getId());
    }
}
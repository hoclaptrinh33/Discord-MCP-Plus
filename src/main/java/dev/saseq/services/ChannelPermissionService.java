package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChannelPermissionService {

    private final JDA jda;

    @Value("${DISCORD_GUILD_ID:}")
    private String defaultGuildId;

    public ChannelPermissionService(JDA jda) {
        this.jda = jda;
    }

    private Guild getGuild(String guildId) {
        if ((guildId == null || guildId.isEmpty()) && defaultGuildId != null && !defaultGuildId.isEmpty())
            guildId = defaultGuildId;
        if (guildId == null || guildId.isEmpty()) throw new IllegalArgumentException("guildId cannot be null");
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) throw new IllegalArgumentException("Discord server not found by guildId");
        return guild;
    }

    private IPermissionContainer getPermissionContainer(Guild guild, String channelId) {
        if (channelId == null || channelId.isEmpty()) throw new IllegalArgumentException("channelId cannot be null");
        GuildChannel channel = guild.getGuildChannelById(channelId);
        if (channel == null) throw new IllegalArgumentException("Channel not found by channelId");
        if (!(channel instanceof IPermissionContainer container))
            throw new IllegalArgumentException("Channel type does not support permission overwrites (threads are not supported)");
        return container;
    }

    private long parsePermissions(String raw, String names) {
        if (raw != null && !raw.isEmpty() && names != null && !names.isEmpty())
            throw new IllegalArgumentException("Cannot specify both raw bitfield and permission names for the same field");
        if (raw != null && !raw.isEmpty()) {
            try { return Long.parseLong(raw); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid permission bitfield: " + raw); }
        }
        if (names != null && !names.isEmpty()) {
            long result = 0;
            for (String name : names.split(",")) {
                try { result |= Permission.valueOf(name.trim()).getRawValue(); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid permission name: " + name.trim()); }
            }
            return result;
        }
        return 0;
    }

    private String formatPermissions(long raw) {
        if (raw == 0) return "none";
        return Permission.getPermissions(raw).stream().map(Permission::getName).collect(Collectors.joining(", "));
    }

    @Tool(name = "list_channel_permission_overwrites", description = "Returns all permission overwrites for a channel with role/member breakdown and allow/deny details")
    public String listChannelPermissionOverwrites(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Channel ID") String channelId) {

        IPermissionContainer container = getPermissionContainer(getGuild(guildId), channelId);
        List<PermissionOverride> overrides = container.getPermissionOverrides();
        if (overrides.isEmpty()) return "No permission overwrites found for this channel.";

        return "Retrieved " + overrides.size() + " permission overwrites:\n" +
                overrides.stream().map(o -> {
                    String type = o.isRoleOverride() ? "Role" : "Member";
                    String name = o.isRoleOverride()
                            ? (o.getRole() != null ? o.getRole().getName() : "Unknown Role")
                            : (o.getMember() != null ? o.getMember().getUser().getName() : "Unknown Member");
                    return String.format("- **%s: %s** (ID: %s)\n  • Allow: %d (%s)\n  • Deny: %d (%s)",
                            type, name, o.getId(),
                            o.getAllowedRaw(), formatPermissions(o.getAllowedRaw()),
                            o.getDeniedRaw(), formatPermissions(o.getDeniedRaw()));
                }).collect(Collectors.joining("\n"));
    }

    private String upsertPermissions(IPermissionContainer container, IPermissionHolder holder,
                                     String targetType, String targetName, String targetId,
                                     String allowRaw, String denyRaw, String allowPerms, String denyPerms, String reason) {
        long allow = parsePermissions(allowRaw, allowPerms);
        long deny = parsePermissions(denyRaw, denyPerms);
        if (allow == 0 && deny == 0)
            throw new IllegalArgumentException("Must specify at least one allow or deny permission");
        if ((allow & deny) != 0)
            throw new IllegalArgumentException("allow and deny cannot contain the same permission bits");

        try {
            var action = container.upsertPermissionOverride(holder).setPermissions(allow, deny);
            if (reason != null && !reason.isEmpty()) action.reason(reason);
            action.complete();
        } catch (HierarchyException e) {
            throw new IllegalArgumentException("Cannot manage overwrite target due to role hierarchy");
        } catch (InsufficientPermissionException e) {
            throw new IllegalArgumentException("Bot lacks permission to manage channel permissions");
        }

        return String.format("Successfully set permission overwrite for %s **%s** (ID: %s):\n• Allow: %d (%s)\n• Deny: %d (%s)",
                targetType, targetName, targetId, allow, formatPermissions(allow), deny, formatPermissions(deny));
    }

    @Tool(name = "upsert_role_channel_permissions", description = "Creates or updates permission overwrite for a role on a channel")
    public String upsertRoleChannelPermissions(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Channel ID") String channelId,
            @ToolParam(description = "Role ID") String roleId,
            @ToolParam(description = "Allow permissions raw bitfield", required = false) String allowRaw,
            @ToolParam(description = "Deny permissions raw bitfield", required = false) String denyRaw,
            @ToolParam(description = "Allow permissions as CSV of names (e.g. VIEW_CHANNEL,MESSAGE_SEND)", required = false) String allowPermissions,
            @ToolParam(description = "Deny permissions as CSV of names (e.g. MESSAGE_SEND,MANAGE_MESSAGES)", required = false) String denyPermissions,
            @ToolParam(description = "Reason for audit log", required = false) String reason) {

        Guild guild = getGuild(guildId);
        IPermissionContainer container = getPermissionContainer(guild, channelId);
        if (roleId == null || roleId.isEmpty()) throw new IllegalArgumentException("roleId cannot be null");
        Role role = guild.getRoleById(roleId);
        if (role == null) throw new IllegalArgumentException("Role not found by roleId");

        return upsertPermissions(container, role, "role", role.getName(), role.getId(),
                allowRaw, denyRaw, allowPermissions, denyPermissions, reason);
    }

    @Tool(name = "upsert_member_channel_permissions", description = "Creates or updates permission overwrite for a member on a channel")
    public String upsertMemberChannelPermissions(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Channel ID") String channelId,
            @ToolParam(description = "User ID") String userId,
            @ToolParam(description = "Allow permissions raw bitfield", required = false) String allowRaw,
            @ToolParam(description = "Deny permissions raw bitfield", required = false) String denyRaw,
            @ToolParam(description = "Allow permissions as CSV of names (e.g. VIEW_CHANNEL,VOICE_CONNECT)", required = false) String allowPermissions,
            @ToolParam(description = "Deny permissions as CSV of names (e.g. MESSAGE_SEND,VOICE_SPEAK)", required = false) String denyPermissions,
            @ToolParam(description = "Reason for audit log", required = false) String reason) {

        Guild guild = getGuild(guildId);
        IPermissionContainer container = getPermissionContainer(guild, channelId);
        if (userId == null || userId.isEmpty()) throw new IllegalArgumentException("userId cannot be null");
        Member member;
        try { member = guild.retrieveMemberById(userId).complete(); }
        catch (ErrorResponseException e) { throw new IllegalArgumentException("User not found in this server by userId"); }

        return upsertPermissions(container, member, "member", member.getUser().getName(), member.getId(),
                allowRaw, denyRaw, allowPermissions, denyPermissions, reason);
    }

    @Tool(name = "delete_channel_permission_overwrite", description = "Deletes a permission overwrite for a role or member from a channel")
    public String deleteChannelPermissionOverwrite(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Channel ID") String channelId,
            @ToolParam(description = "Target type: 'role' or 'member'") String targetType,
            @ToolParam(description = "Target role or user ID") String targetId,
            @ToolParam(description = "Reason for audit log", required = false) String reason) {

        Guild guild = getGuild(guildId);
        IPermissionContainer container = getPermissionContainer(guild, channelId);
        if (targetType == null || targetType.isEmpty()) throw new IllegalArgumentException("targetType cannot be null");
        if (targetId == null || targetId.isEmpty()) throw new IllegalArgumentException("targetId cannot be null");
        if (!targetType.equals("role") && !targetType.equals("member"))
            throw new IllegalArgumentException("targetType must be 'role' or 'member'");

        PermissionOverride override = container.getPermissionOverrides().stream()
                .filter(o -> o.getId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No permission overwrite found for target ID: " + targetId));

        if (targetType.equals("role") && !override.isRoleOverride())
            throw new IllegalArgumentException("Target ID refers to a member overwrite, not a role overwrite");
        if (targetType.equals("member") && !override.isMemberOverride())
            throw new IllegalArgumentException("Target ID refers to a role overwrite, not a member overwrite");

        String targetName = override.isRoleOverride()
                ? (override.getRole() != null ? override.getRole().getName() : "Unknown Role")
                : (override.getMember() != null ? override.getMember().getUser().getName() : "Unknown Member");

        try {
            var action = override.delete();
            if (reason != null && !reason.isEmpty()) action.reason(reason);
            action.complete();
        } catch (InsufficientPermissionException e) {
            throw new IllegalArgumentException("Bot lacks permission to manage channel permissions");
        }

        return String.format("Successfully deleted permission overwrite for %s **%s** (ID: %s) from channel", targetType, targetName, targetId);
    }

    // ==================== NEW: Part 1 - Effective Permissions ====================

    /**
     * Get the effective permissions a member has in a channel or category.
     * This combines base role permissions + channel/category overwrites.
     */
    @Tool(name = "get_effective_permissions", description = "Get the list of effective permissions a user actually has in a specific channel or category (roles + overwrites)")
    public String getEffectivePermissions(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Channel or Category ID") String channelId,
            @ToolParam(description = "User ID to check permissions for") String userId) {

        Guild guild = getGuild(guildId);
        GuildChannel channel = guild.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel/Category not found by channelId");
        }

        Member member;
        try {
            member = guild.retrieveMemberById(userId).complete();
        } catch (ErrorResponseException e) {
            throw new IllegalArgumentException("User not found in this server");
        }

        // Get effective permissions for this channel
        java.util.EnumSet<Permission> effectivePerms = member.getPermissions(channel);

        StringBuilder sb = new StringBuilder();
        sb.append("**Effective permissions for ").append(member.getUser().getName())
          .append("** in **").append(channel.getName()).append("**:\n\n");

        if (effectivePerms.isEmpty()) {
            sb.append("No permissions.");
        } else {
            effectivePerms.stream()
                .sorted(java.util.Comparator.comparing(Permission::getName))
                .forEach(p -> sb.append("- `").append(p.getName()).append("`\n"));
        }

        long raw = Permission.getRaw(effectivePerms);
        sb.append("\n**Raw bitfield:** ").append(raw);

        return sb.toString();
    }

    // ==================== NEW: Part 2 - Sync Permissions from Category ====================

    /**
     * Sync a channel's permission overwrites to exactly match its parent category.
     * This is equivalent to the "Sync Permissions" button in Discord.
     */
    @Tool(name = "sync_channel_permissions_with_category", description = "Sync a channel's permission overwrites to match its parent category (like Discord's Sync Permissions)")
    public String syncChannelPermissionsWithCategory(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Channel ID to sync (must be under a category)") String channelId,
            @ToolParam(description = "Reason for audit log", required = false) String reason) {

        Guild guild = getGuild(guildId);
        GuildChannel channel = guild.getGuildChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found");
        }

        if (!(channel instanceof ICategorizableChannel categorizable)) {
            throw new IllegalArgumentException("This channel type cannot belong to a category");
        }

        Category category = categorizable.getParentCategory();
        if (category == null) {
            return "This channel does not belong to any category. Nothing to sync.";
        }

        if (!(channel instanceof IPermissionContainer targetContainer)) {
            throw new IllegalArgumentException("Target channel does not support permission overwrites");
        }

        IPermissionContainer categoryContainer = category;

        // Get current overrides on the target channel
        List<PermissionOverride> currentOverrides = targetContainer.getPermissionOverrides();
        Set<String> currentTargetIds = currentOverrides.stream()
                .map(PermissionOverride::getId)
                .collect(java.util.stream.Collectors.toSet());

        int synced = 0;
        int removed = 0;

        // 1. Apply all category overwrites to the target channel
        for (PermissionOverride catOverride : categoryContainer.getPermissionOverrides()) {
            IPermissionHolder holder;
            if (catOverride.isRoleOverride()) {
                holder = catOverride.getRole();
            } else {
                holder = catOverride.getMember();
            }

            if (holder == null) continue;

            try {
                targetContainer.upsertPermissionOverride(holder)
                        .setPermissions(catOverride.getAllowedRaw(), catOverride.getDeniedRaw())
                        .reason(reason != null ? reason : "Synced permissions from category")
                        .complete();

                currentTargetIds.remove(catOverride.getId());
                synced++;
            } catch (Exception e) {
                // Log but continue
            }
        }

        // 2. Remove overrides that exist on the channel but not in the category
        for (String extraId : currentTargetIds) {
            PermissionOverride extraOverride = currentOverrides.stream()
                    .filter(o -> o.getId().equals(extraId))
                    .findFirst()
                    .orElse(null);

            if (extraOverride != null) {
                try {
                    extraOverride.delete()
                            .reason(reason != null ? reason : "Synced permissions from category")
                            .complete();
                    removed++;
                } catch (Exception e) {
                    // continue
                }
            }
        }

        return String.format("Successfully synced permissions for channel **%s** from category **%s**.\n" +
                        "• Synced/Updated: %d overwrites\n• Removed extra: %d overwrites",
                channel.getName(), category.getName(), synced, removed);
    }

    /**
     * Optional: Sync permissions for ALL channels under a category.
     */
    @Tool(name = "sync_all_channels_in_category", description = "Sync permissions for every channel under a category to match the category")
    public String syncAllChannelsInCategory(
            @ToolParam(description = "Discord server ID", required = false) String guildId,
            @ToolParam(description = "Category ID") String categoryId,
            @ToolParam(description = "Reason for audit log", required = false) String reason) {

        Guild guild = getGuild(guildId);
        Category category = guild.getCategoryById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }

        List<GuildChannel> children = guild.getChannels().stream()
                .filter(ch -> ch instanceof ICategorizableChannel)
                .map(ch -> (ICategorizableChannel) ch)
                .filter(ch -> category.equals(ch.getParentCategory()))
                .map(ch -> (GuildChannel) ch)
                .collect(java.util.stream.Collectors.toList());

        if (children.isEmpty()) {
            return "No channels found under this category.";
        }

        int success = 0;
        StringBuilder errors = new StringBuilder();

        for (GuildChannel child : children) {
            try {
                // Reuse the logic by calling internal or duplicate minimal code
                if (child instanceof IPermissionContainer targetContainer) {
                    // Apply all from category
                    for (PermissionOverride catOv : category.getPermissionOverrides()) {
                        IPermissionHolder holder = catOv.isRoleOverride() ? catOv.getRole() : catOv.getMember();
                        if (holder != null) {
                            targetContainer.upsertPermissionOverride(holder)
                                    .setPermissions(catOv.getAllowedRaw(), catOv.getDeniedRaw())
                                    .reason(reason)
                                    .complete();
                        }
                    }

                    // Remove extras
                    Set<String> catIds = category.getPermissionOverrides().stream()
                            .map(PermissionOverride::getId).collect(java.util.stream.Collectors.toSet());

                    for (PermissionOverride childOv : targetContainer.getPermissionOverrides()) {
                        if (!catIds.contains(childOv.getId())) {
                            childOv.delete().reason(reason).complete();
                        }
                    }
                    success++;
                }
            } catch (Exception e) {
                errors.append("\n- Failed for ").append(child.getName()).append(": ").append(e.getMessage());
            }
        }

        return String.format("Synced %d/%d channels under category **%s**.%s",
                success, children.size(), category.getName(),
                errors.length() > 0 ? "\nErrors:" + errors : "");
    }
}

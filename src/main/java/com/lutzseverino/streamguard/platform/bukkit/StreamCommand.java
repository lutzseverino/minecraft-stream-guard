package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.application.StreamProviderRegistry;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import com.lutzseverino.streamguard.i18n.MessageService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class StreamCommand implements CommandExecutor, TabCompleter {

    private final StreamService streamService;
    private final StreamProviderRegistry providerRegistry;
    private final MessageService messages;
    private final JavaPlugin plugin;

    public StreamCommand(
            StreamService streamService,
            StreamProviderRegistry providerRegistry,
            MessageService messages,
            JavaPlugin plugin
    ) {
        this.streamService = streamService;
        this.providerRegistry = providerRegistry;
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            sendStatus(player, streamService.status(player.getUniqueId(), player.getName()));
            return true;
        }
        if ("link".equalsIgnoreCase(args[0]) && args.length >= 3) {
            StreamProviderId providerId = StreamProviderId.parse(args[1]).orElse(null);
            if (providerId == null || StreamProviderId.MANUAL.equals(providerId)) {
                player.sendMessage(messages.renderDefault("stream.link.invalid-platform", Map.of()));
                return true;
            }
            if (!providerRegistry.linkable(providerId)) {
                player.sendMessage(messages.renderDefault("stream.link.unsupported-platform", Map.of(
                        "platform", providerId.displayName()
                )));
                return true;
            }
            PlayerAccessRecord record = streamService.link(player.getUniqueId(), player.getName(), providerId, args[2]);
            player.sendMessage(messages.renderDefault("stream.link.saved", Map.of(
                    "platform", record.streamLink().providerId().displayName(),
                    "channel", record.streamLink().channel()
            )));
            return true;
        }
        if ("verify".equalsIgnoreCase(args[0])) {
            UUID playerId = player.getUniqueId();
            String playerName = player.getName();
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                VerificationStatus status = streamService.verify(playerId, playerName);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player online = plugin.getServer().getPlayer(playerId);
                    if (online != null && online.isOnline()) {
                        sendVerification(online, status);
                    }
                });
            });
            return true;
        }
        player.sendMessage(messages.renderDefault("stream.usage", Map.of("label", label)));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return List.of("status", "link", "verify");
        }
        if (args.length == 2 && "link".equalsIgnoreCase(args[0])) {
            return providerRegistry.linkableProviderIds();
        }
        return List.of();
    }

    private void sendStatus(Player player, PlayerAccessRecord record) {
        record.verificationStatusOptional().ifPresentOrElse(
                status -> sendVerification(player, status),
                () -> player.sendMessage(messages.renderDefault("stream.unverified", Map.of()))
        );
    }

    private void sendVerification(Player player, VerificationStatus status) {
        if (status.live()) {
            player.sendMessage(messages.renderDefault("stream.verified", Map.of(
                    "platform", status.verifiedProviderId().map(StreamProviderId::displayName).orElse("Unknown")
            )));
        } else {
            player.sendMessage(messages.renderDefault("stream.unverified", Map.of(
                    "detail", status.detail().toLowerCase(Locale.ROOT)
            )));
        }
    }
}

package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.application.StreamProviderRegistry;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.i18n.MessageService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StreamCommand implements CommandExecutor, TabCompleter {

    private final StreamService streamService;
    private final StreamProviderRegistry providerRegistry;
    private final BukkitOnboardingFlow onboardingFlow;
    private final BukkitStreamVerificationRunner verificationRunner;
    private final MessageService messages;

    public StreamCommand(
            StreamService streamService,
            StreamProviderRegistry providerRegistry,
            BukkitOnboardingFlow onboardingFlow,
            BukkitStreamVerificationRunner verificationRunner,
            MessageService messages
    ) {
        this.streamService = streamService;
        this.providerRegistry = providerRegistry;
        this.onboardingFlow = onboardingFlow;
        this.verificationRunner = verificationRunner;
        this.messages = messages;
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
        if ("setup".equalsIgnoreCase(args[0])) {
            onboardingFlow.open(player);
            return true;
        }
        if ("cancel".equalsIgnoreCase(args[0])) {
            onboardingFlow.cancel(player, true);
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
            String linkReference = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            PlayerAccessRecord accessRecord = streamService.link(player.getUniqueId(), player.getName(), providerId, linkReference);
            player.sendMessage(messages.renderDefault("stream.link.saved", Map.of(
                    "platform", accessRecord.streamLink().providerId().displayName(),
                    "channel", accessRecord.streamLink().channel()
            )));
            verificationRunner.verify(player);
            return true;
        }
        if ("verify".equalsIgnoreCase(args[0])) {
            verificationRunner.verify(player);
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
            return List.of("status", "setup", "link", "verify", "cancel");
        }
        if (args.length == 2 && "link".equalsIgnoreCase(args[0])) {
            return providerRegistry.linkableProviderIds();
        }
        return List.of();
    }

    private void sendStatus(Player player, PlayerAccessRecord accessRecord) {
        if (accessRecord.streamLinkOptional().isEmpty()) {
            player.sendMessage(messages.renderDefault("stream.unlinked", Map.of()));
            return;
        }
        accessRecord.verificationStatusOptional().ifPresentOrElse(
                status -> verificationRunner.sendVerification(player, status),
                () -> player.sendMessage(messages.renderDefault("stream.not-checked", Map.of()))
        );
    }
}

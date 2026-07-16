package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import com.lutzseverino.streamguard.i18n.MessageService;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitStreamVerificationRunner {

  private final JavaPlugin plugin;
  private final StreamService streamService;
  private final MessageService messages;

  public BukkitStreamVerificationRunner(
      JavaPlugin plugin, StreamService streamService, MessageService messages) {
    this.plugin = plugin;
    this.streamService = streamService;
    this.messages = messages;
  }

  public void verify(Player player) {
    UUID playerId = player.getUniqueId();
    String playerName = player.getName();
    if (streamService.status(playerId, playerName).streamLinkOptional().isEmpty()) {
      player.sendMessage(messages.renderLegacyDefault("stream.unlinked", Map.of()));
      return;
    }
    player.sendMessage(messages.renderLegacyDefault("stream.verify.started", Map.of()));
    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              VerificationStatus status = streamService.verify(playerId, playerName);
              plugin
                  .getServer()
                  .getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        Player online = plugin.getServer().getPlayer(playerId);
                        if (online != null && online.isOnline()) {
                          sendVerification(online, status);
                        }
                      });
            });
  }

  public void sendVerification(Player player, VerificationStatus status) {
    if (status.live()) {
      player.sendMessage(
          messages.renderLegacyDefault(
              "stream.live",
              Map.of(
                  "platform",
                  status
                      .verifiedProviderId()
                      .map(StreamProviderId::displayName)
                      .orElse("Unknown"))));
      return;
    }
    player.sendMessage(
        messages.renderLegacyDefault(
            "stream.not-live",
            Map.of(
                "detail",
                status.detail().isBlank() ? "No active stream was found." : status.detail())));
  }
}

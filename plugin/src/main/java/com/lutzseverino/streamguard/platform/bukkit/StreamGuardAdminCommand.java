package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.application.BypassService;
import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.config.StreamGuardSettings;
import com.lutzseverino.streamguard.domain.BypassGrant;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.i18n.MessageService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StreamGuardAdminCommand implements CommandExecutor, TabCompleter {

  private final BypassService bypassService;
  private final StreamService streamService;
  private final MessageService messages;
  private final StreamGuardSettings settings;
  private final Runnable reloadAction;

  public StreamGuardAdminCommand(
      BypassService bypassService,
      StreamService streamService,
      MessageService messages,
      StreamGuardSettings settings,
      Runnable reloadAction) {
    this.bypassService = bypassService;
    this.streamService = streamService;
    this.messages = messages;
    this.settings = settings;
    this.reloadAction = reloadAction;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (args.length == 0) {
      sender.sendMessage(messages.renderLegacyDefault("admin.usage", Map.of("label", label)));
      return true;
    }
    if ("reload".equalsIgnoreCase(args[0])) {
      if (lacksPermission(sender, "streamguard.reload")) {
        return true;
      }
      reloadAction.run();
      sender.sendMessage(messages.renderLegacyDefault("system.reloaded", Map.of()));
      return true;
    }
    if ("status".equalsIgnoreCase(args[0]) && args.length >= 2) {
      if (lacksPermission(sender, "streamguard.status.others")) {
        return true;
      }
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      PlayerAccessRecord accessRecord =
          streamService.status(target.getUniqueId(), target.getName());
      String state =
          accessRecord
              .verificationStatusOptional()
              .map(status -> status.live() ? "live" : "not live")
              .orElse("unknown");
      sender.sendMessage(
          messages.renderLegacyDefault(
              "admin.status", Map.of("player", displayName(target), "state", state)));
      return true;
    }
    if ("verify".equalsIgnoreCase(args[0]) && args.length >= 2) {
      if (lacksPermission(sender, "streamguard.admin")) {
        return true;
      }
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      streamService.manuallyVerify(target.getUniqueId(), target.getName(), reason(args, 2));
      sender.sendMessage(
          messages.renderLegacyDefault(
              "admin.verify.added", Map.of("player", displayName(target))));
      return true;
    }
    if ("unverify".equalsIgnoreCase(args[0]) && args.length >= 2) {
      if (lacksPermission(sender, "streamguard.admin")) {
        return true;
      }
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
      streamService.unverify(target.getUniqueId(), target.getName(), reason(args, 2));
      sender.sendMessage(
          messages.renderLegacyDefault(
              "admin.verify.removed", Map.of("player", displayName(target))));
      return true;
    }
    if ("bypass".equalsIgnoreCase(args[0]) && args.length >= 2) {
      handleBypass(sender, args);
      return true;
    }
    sender.sendMessage(messages.renderLegacyDefault("admin.usage", Map.of("label", label)));
    return true;
  }

  @Override
  public @NotNull List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    if (args.length == 1) {
      return List.of("status", "bypass", "verify", "unverify", "reload");
    }
    if (args.length == 2 && "bypass".equalsIgnoreCase(args[0])) {
      return List.of("grant", "remove");
    }
    return List.of();
  }

  private void handleBypass(CommandSender sender, String[] args) {
    if (lacksPermission(sender, "streamguard.bypass")) {
      return;
    }
    if ("remove".equalsIgnoreCase(args[1]) && args.length >= 3) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
      bypassService.revoke(target.getUniqueId(), target.getName());
      sender.sendMessage(
          messages.renderLegacyDefault(
              "admin.bypass.removed", Map.of("player", displayName(target))));
      return;
    }
    if ("grant".equalsIgnoreCase(args[1]) && args.length >= 3) {
      OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
      Duration duration = null;
      int reasonStart = 3;
      if (args.length >= 4) {
        var parsed = DurationParser.parse(args[3]);
        if (parsed.isPresent()) {
          Duration parsedDuration = parsed.get();
          if (!parsedDuration.isZero()) {
            if (!settings.bypass().allowTemporaryBypass()) {
              sender.sendMessage(
                  messages.renderLegacyDefault("admin.bypass.temporary-disabled", Map.of()));
              return;
            }
            int maxMinutes = settings.bypass().maxTemporaryBypassMinutes();
            if (maxMinutes > 0 && parsedDuration.toMinutes() > maxMinutes) {
              sender.sendMessage(
                  messages.renderLegacyDefault(
                      "admin.bypass.too-long", Map.of("minutes", Integer.toString(maxMinutes))));
              return;
            }
            duration = parsedDuration;
          }
          reasonStart = 4;
        }
      }
      UUID grantedBy = sender instanceof Player player ? player.getUniqueId() : null;
      BypassGrant grant =
          bypassService.grant(
              target.getUniqueId(),
              target.getName(),
              grantedBy,
              duration,
              reason(args, reasonStart));
      sender.sendMessage(
          messages.renderLegacyDefault(
              "admin.bypass.added",
              Map.of(
                  "player",
                  displayName(target),
                  "duration",
                  grant.temporary() ? grant.expiresAt().toString() : "permanent")));
      return;
    }
    sender.sendMessage(messages.renderLegacyDefault("admin.usage", Map.of("label", "streamguard")));
  }

  private boolean lacksPermission(CommandSender sender, String permission) {
    if (!sender.hasPermission(permission)) {
      sender.sendMessage(messages.renderLegacyDefault("system.no-permission", Map.of()));
      return true;
    }
    return false;
  }

  private static String reason(String[] args, int start) {
    if (args.length <= start) {
      return "";
    }
    return String.join(" ", Arrays.copyOfRange(args, start, args.length));
  }

  private static String displayName(OfflinePlayer player) {
    return player.getName() == null ? player.getUniqueId().toString() : player.getName();
  }
}

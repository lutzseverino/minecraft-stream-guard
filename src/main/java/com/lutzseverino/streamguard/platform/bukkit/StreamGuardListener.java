package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.application.AccessService;
import com.lutzseverino.streamguard.application.SessionRegistry;
import com.lutzseverino.streamguard.config.StreamGuardSettings;
import com.lutzseverino.streamguard.domain.AccessDecision;
import com.lutzseverino.streamguard.domain.GateState;
import com.lutzseverino.streamguard.domain.GuardedAction;
import com.lutzseverino.streamguard.i18n.MessageService;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public final class StreamGuardListener implements Listener {

    private final AccessService accessService;
    private final SessionRegistry sessionRegistry;
    private final MessageService messages;
    private final StreamGuardSettings settings;
    private final JavaPlugin plugin;

    public StreamGuardListener(
            AccessService accessService,
            SessionRegistry sessionRegistry,
            MessageService messages,
            StreamGuardSettings settings,
            JavaPlugin plugin
    ) {
        this.accessService = accessService;
        this.sessionRegistry = sessionRegistry;
        this.messages = messages;
        this.settings = settings;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionRegistry.playerJoined(player.getUniqueId());
        maybeScheduleKick(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sessionRegistry.playerLeft(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (sameBlock(event)) {
            return;
        }
        Player player = event.getPlayer();
        GateState state = gateState(player);
        if (stateRules(state).allowMovement()) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(messages.renderDefault(messageKey(state, "movement"), Map.of()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GateState state = gateState(player);
        if (stateRules(state).allowChat()) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(messages.renderDefault(messageKey(state, "chat"), Map.of()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        GateState state = gateState(player);
        if (stateRules(state).allowCommands() || safeCommand(event.getMessage())) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage(messages.renderDefault(messageKey(state, "command"), Map.of()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        cancelIfDenied(event.getPlayer(), GuardedAction.BLOCK_BREAK, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        cancelIfDenied(event.getPlayer(), GuardedAction.BLOCK_PLACE, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        GuardedAction action = isContainerLike(event.getClickedBlock().getType())
                ? GuardedAction.CONTAINER_OPEN
                : GuardedAction.BLOCK_INTERACT;
        cancelIfDenied(event.getPlayer(), action, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            GuardedAction action = event.getInventory().getType().name().contains("MERCHANT")
                    ? GuardedAction.VILLAGER_TRADING
                    : GuardedAction.CONTAINER_OPEN;
            cancelIfDenied(player, action, () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            cancelIfDenied(player, GuardedAction.INVENTORY_CLICK, () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            cancelIfDenied(player, GuardedAction.CRAFTING, () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        cancelIfDenied(event.getPlayer(), GuardedAction.ITEM_DROP, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            cancelIfDenied(player, GuardedAction.ITEM_PICKUP, () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player player = damagingPlayer(event.getDamager());
        if (player != null) {
            cancelIfDenied(player, GuardedAction.ENTITY_DAMAGE, () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        GuardedAction action = event.getRightClicked() instanceof AbstractVillager
                ? GuardedAction.VILLAGER_TRADING
                : GuardedAction.ENTITY_INTERACT;
        cancelIfDenied(event.getPlayer(), action, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        cancelIfDenied(event.getPlayer(), GuardedAction.BUCKETS, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        cancelIfDenied(event.getPlayer(), GuardedAction.BUCKETS, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            cancelIfDenied(player, GuardedAction.FIRE, () -> event.setCancelled(true));
        }
    }

    private void cancelIfDenied(Player player, GuardedAction action, Cancellation cancellation) {
        AccessDecision decision = accessService.decide(
                player.getUniqueId(),
                player.getName(),
                action,
                hasPermissionBypass(player)
        );
        if (decision.allowed()) {
            return;
        }
        cancellation.cancel();
        GateState state = gateState(player);
        player.sendMessage(messages.renderDefault(messageKey(state, "interaction"), Map.of("action", action.configKey())));
    }

    private boolean hasPermissionBypass(Player player) {
        return player.hasPermission("streamguard.bypass.always")
                || (settings.bypass().opsBypassByDefault() && player.isOp());
    }

    private GateState gateState(Player player) {
        return accessService.gateState(player.getUniqueId(), player.getName(), hasPermissionBypass(player));
    }

    private StreamGuardSettings.StateRules stateRules(GateState state) {
        return switch (state) {
            case BYPASSED, VERIFIED -> new StreamGuardSettings.StateRules(false, 0, true, true, true);
            case UNLINKED -> settings.enforcement().unlinked();
            case NOT_LIVE -> settings.enforcement().notLive();
        };
    }

    private void maybeScheduleKick(Player player) {
        GateState state = gateState(player);
        StreamGuardSettings.StateRules rules = stateRules(state);
        if (!rules.kickOnJoin()) {
            return;
        }
        long ticks = Math.max(1L, rules.kickDelaySeconds() * 20L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            GateState current = gateState(player);
            if (stateRules(current).kickOnJoin()) {
                player.kickPlayer(messages.renderPlainDefault(messageKey(current, "kick"), Map.of()));
            }
        }, ticks);
    }

    private boolean safeCommand(String rawMessage) {
        String command = rawMessage.startsWith("/") ? rawMessage.substring(1) : rawMessage;
        String normalized = command.toLowerCase(java.util.Locale.ROOT);
        return settings.commandSafety().safeWhileUnverified().stream()
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .anyMatch(safe -> normalized.equals(safe) || normalized.startsWith(safe + " "));
    }

    private static String messageKey(GateState state, String action) {
        return switch (state) {
            case UNLINKED -> "stream.locked.unlinked." + action;
            case NOT_LIVE -> "stream.locked.not-live." + action;
            case BYPASSED, VERIFIED -> "stream.locked.interaction";
        };
    }

    private static Player damagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private static boolean isContainerLike(Material material) {
        String name = material.name();
        return name.contains("CHEST")
                || name.contains("SHULKER")
                || name.contains("BARREL")
                || name.contains("FURNACE")
                || name.contains("HOPPER")
                || name.contains("DISPENSER")
                || name.contains("DROPPER")
                || name.contains("ANVIL")
                || name.contains("TABLE")
                || name.contains("BREWING_STAND")
                || name.contains("LECTERN");
    }

    private static boolean sameBlock(PlayerMoveEvent event) {
        return event.getFrom().getWorld().equals(event.getTo().getWorld())
                && event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ();
    }

    @FunctionalInterface
    private interface Cancellation {
        void cancel();
    }
}

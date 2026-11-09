package com.lutzseverino.streamguard.platform.bukkit;

import com.lutzseverino.streamguard.application.StreamProviderRegistry;
import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.config.StreamGuardSettings;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.i18n.MessageService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("deprecation")
public final class BukkitOnboardingFlow implements Listener {

  private final JavaPlugin plugin;
  private final StreamService streamService;
  private final StreamProviderRegistry providerRegistry;
  private final BukkitStreamVerificationRunner verificationRunner;
  private final MessageService messages;
  private final StreamGuardSettings.Onboarding settings;
  private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

  public BukkitOnboardingFlow(
      JavaPlugin plugin,
      StreamService streamService,
      StreamProviderRegistry providerRegistry,
      BukkitStreamVerificationRunner verificationRunner,
      MessageService messages,
      StreamGuardSettings.Onboarding settings) {
    this.plugin = plugin;
    this.streamService = streamService;
    this.providerRegistry = providerRegistry;
    this.verificationRunner = verificationRunner;
    this.messages = messages;
    this.settings = settings;
  }

  public void open(Player player) {
    if (!settings.enabled()) {
      player.sendMessage(messages.renderLegacyDefault("stream.setup.disabled", Map.of()));
      return;
    }
    List<StreamGuardSettings.ProviderButton> buttons =
        settings.providerPicker().providers().stream()
            .filter(StreamGuardSettings.ProviderButton::enabled)
            .filter(button -> providerRegistry.linkable(button.providerId()))
            .toList();
    if (buttons.isEmpty()) {
      player.sendMessage(messages.renderLegacyDefault("stream.setup.no-providers", Map.of()));
      return;
    }

    int size = settings.providerPicker().rows() * 9;
    ProviderPickerHolder holder = new ProviderPickerHolder();
    Inventory inventory =
        Bukkit.createInventory(
            holder,
            size,
            messages.renderLegacyTemplate(settings.providerPicker().title(), Map.of()));
    holder.setInventory(inventory);

    if (settings.providerPicker().fillEmptySlots()) {
      ItemStack filler = item(settings.providerPicker().filler(), Map.of());
      for (int slot = 0; slot < size; slot++) {
        inventory.setItem(slot, filler);
      }
    }

    place(
        inventory,
        settings.providerPicker().cancel().slot(),
        item(settings.providerPicker().cancel(), Map.of()));
    holder.setCancelSlot(settings.providerPicker().cancel().slot());

    for (StreamGuardSettings.ProviderButton button : buttons) {
      int slot = button.slot();
      if (slot < 0 || slot >= size) {
        continue;
      }
      Map<String, String> placeholders =
          Map.of(
              "platform", button.providerId().displayName(),
              "provider", button.providerId().value(),
              "input_hint", button.inputHint());
      inventory.setItem(slot, item(button.item(), placeholders));
      holder.addProvider(slot, button.providerId(), button.inputHint());
    }

    player.openInventory(inventory);
  }

  public void cancel(Player player, boolean notify) {
    PendingInput input = pendingInputs.remove(player.getUniqueId());
    if (input != null) {
      input.timeoutTask().cancel();
      if (notify) {
        player.sendMessage(messages.renderLegacyDefault("stream.setup.input.cancelled", Map.of()));
      }
      return;
    }
    if (notify) {
      player.sendMessage(messages.renderLegacyDefault("stream.setup.input.none", Map.of()));
    }
  }

  public void shutdown() {
    pendingInputs.values().forEach(input -> input.timeoutTask().cancel());
    pendingInputs.clear();
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (!(event.getView().getTopInventory().getHolder() instanceof ProviderPickerHolder holder)) {
      return;
    }
    event.setCancelled(true);
    if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
      return;
    }
    if (event.getRawSlot() == holder.cancelSlot()) {
      player.closeInventory();
      player.sendMessage(messages.renderLegacyDefault("stream.setup.input.cancelled", Map.of()));
      return;
    }
    ProviderChoice choice = holder.providerAt(event.getRawSlot());
    if (choice == null) {
      return;
    }
    player.closeInventory();
    startChatInput(player, choice);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof ProviderPickerHolder) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChatInput(AsyncPlayerChatEvent event) {
    PendingInput pending = pendingInputs.get(event.getPlayer().getUniqueId());
    if (pending == null) {
      return;
    }
    event.setCancelled(true);
    String input = event.getMessage();
    plugin
        .getServer()
        .getScheduler()
        .runTask(plugin, () -> completeChatInput(event.getPlayer(), pending, input));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    cancel(event.getPlayer(), false);
  }

  private void startChatInput(Player player, ProviderChoice choice) {
    cancel(player, false);
    var timeoutTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  PendingInput removed = pendingInputs.remove(player.getUniqueId());
                  if (removed != null && player.isOnline()) {
                    player.sendMessage(
                        messages.renderLegacyDefault("stream.setup.input.expired", Map.of()));
                  }
                },
                settings.chatInput().timeoutSeconds() * 20L);
    pendingInputs.put(
        player.getUniqueId(),
        new PendingInput(choice.providerId(), choice.inputHint(), timeoutTask));
    player.sendMessage(
        messages.renderLegacyDefault(
            "stream.setup.input.prompt",
            Map.of(
                "platform", choice.providerId().displayName(),
                "input_hint", choice.inputHint(),
                "cancel", settings.chatInput().cancelKeyword())));
  }

  private void completeChatInput(Player player, PendingInput pending, String input) {
    String trimmed = input.trim();
    if (trimmed.equalsIgnoreCase(settings.chatInput().cancelKeyword())) {
      cancel(player, true);
      return;
    }
    if (trimmed.isBlank()) {
      player.sendMessage(
          messages.renderLegacyDefault(
              "stream.setup.input.empty",
              Map.of(
                  "platform", pending.providerId().displayName(),
                  "input_hint", pending.inputHint(),
                  "cancel", settings.chatInput().cancelKeyword())));
      return;
    }
    if (trimmed.length() > settings.chatInput().maxLength()) {
      player.sendMessage(
          messages.renderLegacyDefault(
              "stream.setup.input.too-long",
              Map.of("max", Integer.toString(settings.chatInput().maxLength()))));
      return;
    }
    PendingInput removed = pendingInputs.remove(player.getUniqueId());
    if (removed == null) {
      return;
    }
    removed.timeoutTask().cancel();
    PlayerAccessRecord accessRecord =
        streamService.link(player.getUniqueId(), player.getName(), removed.providerId(), trimmed);
    player.sendMessage(
        messages.renderLegacyDefault(
            "stream.link.saved",
            Map.of(
                "platform", accessRecord.streamLink().providerId().displayName(),
                "channel", accessRecord.streamLink().channel())));
    if (settings.chatInput().verifyAfterLink()) {
      verificationRunner.verify(player);
    }
  }

  private ItemStack item(StreamGuardSettings.GuiItem item, Map<String, String> placeholders) {
    Material material = Material.matchMaterial(item.material());
    if (material == null || material.isAir()) {
      material = Material.STONE;
    }
    ItemStack stack = new ItemStack(material);
    ItemMeta meta = stack.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(messages.renderLegacyTemplate(item.name(), placeholders));
      if (!item.lore().isEmpty()) {
        meta.setLore(
            item.lore().stream()
                .map(line -> messages.renderLegacyTemplate(line, placeholders))
                .toList());
      }
      if (item.customModelData() > 0) {
        meta.setCustomModelData(item.customModelData());
      }
      if (item.glow()) {
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      }
      stack.setItemMeta(meta);
    }
    return stack;
  }

  private static void place(Inventory inventory, int slot, ItemStack item) {
    if (slot >= 0 && slot < inventory.getSize()) {
      inventory.setItem(slot, item);
    }
  }

  private record PendingInput(
      StreamProviderId providerId, String inputHint, org.bukkit.scheduler.BukkitTask timeoutTask) {}

  private record ProviderChoice(StreamProviderId providerId, String inputHint) {}

  private static final class ProviderPickerHolder implements InventoryHolder {

    private final Map<Integer, ProviderChoice> providersBySlot = new HashMap<>();
    private Inventory inventory;
    private int cancelSlot = -1;

    private void setInventory(Inventory inventory) {
      this.inventory = inventory;
    }

    private void setCancelSlot(int cancelSlot) {
      this.cancelSlot = cancelSlot;
    }

    private int cancelSlot() {
      return cancelSlot;
    }

    private void addProvider(int slot, StreamProviderId providerId, String inputHint) {
      providersBySlot.put(slot, new ProviderChoice(providerId, inputHint));
    }

    private ProviderChoice providerAt(int slot) {
      return providersBySlot.get(slot);
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }
}

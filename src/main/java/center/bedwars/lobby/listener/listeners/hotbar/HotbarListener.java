package center.bedwars.lobby.listener.listeners.hotbar;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.ItemsConfiguration;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import center.bedwars.lobby.manager.orphans.PlayerVisibilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class HotbarListener implements Listener {

    private final HotbarManager hotbarManager;
    private final PlayerVisibilityManager visibilityManager;

    public HotbarListener() {
        this.hotbarManager = Lobby.getManagerStorage().getManager(HotbarManager.class);
        this.visibilityManager = Lobby.getManagerStorage().getManager(PlayerVisibilityManager.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        hotbarManager.giveLobbyHotbar(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DISPLAY_NAME))) {
            event.setCancelled(true);
            sendHotbarMessage(player, LanguageConfiguration.HOTBAR.QUICK_PLAY);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME))) {
            event.setCancelled(true);
            sendHotbarMessage(player, LanguageConfiguration.HOTBAR.PROFILE);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DISPLAY_NAME))) {
            event.setCancelled(true);
            sendHotbarMessage(player, LanguageConfiguration.HOTBAR.BEDWARS_MENU);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.SHOP.DISPLAY_NAME))) {
            event.setCancelled(true);
            sendHotbarMessage(player, LanguageConfiguration.HOTBAR.SHOP);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DISPLAY_NAME))) {
            event.setCancelled(true);
            sendHotbarMessage(player, LanguageConfiguration.HOTBAR.COLLECTIBLES);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DISPLAY_NAME))) {
            event.setCancelled(true);
            if (!visibilityManager.toggleVisibilityWithCooldown(player)) {
                long remaining = visibilityManager.getRemainingCooldown(player);
                String seconds = formatSeconds(remaining);
                sendHotbarMessage(player, LanguageConfiguration.HOTBAR.VISIBILITY_COOLDOWN
                        .replace("%seconds%", seconds));
                return;
            }

            hotbarManager.updateHotbar(player);
            if (visibilityManager.isHidden(player)) {
                sendHotbarMessage(player, LanguageConfiguration.HOTBAR.VISIBILITY_HIDDEN);
            } else {
                sendHotbarMessage(player, LanguageConfiguration.HOTBAR.VISIBILITY_VISIBLE);
            }
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DISPLAY_NAME))) {
            event.setCancelled(true);
            sendHotbarMessage(player, LanguageConfiguration.HOTBAR.LOBBY_SELECTOR);
        }
    }

    private void sendHotbarMessage(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ColorUtil.sendMessage(player, message);
    }

    private String formatSeconds(long millis) {
        if (millis <= 0) {
            return "0";
        }
        double seconds = millis / 1000.0D;
        if (seconds >= 1) {
            return String.format("%.1f", seconds);
        }
        return String.format("%.2f", seconds);
    }
}
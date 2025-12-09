package center.bedwars.lobby.listener.listeners.hotbar;

import center.bedwars.lobby.configuration.configurations.ItemsConfiguration;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.hotbar.IHotbarService;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.visibility.IPlayerVisibilityService;
import com.google.inject.Inject;
import com.yapzhenyie.GadgetsMenu.api.GadgetsMenuAPI;
import com.yapzhenyie.GadgetsMenu.player.PlayerManager;
import center.bedwars.lobby.menu.IMenuService;
import de.marcely.bedwars.api.cosmetics.CosmeticsAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class HotbarListener implements Listener {

    private final IHotbarService hotbarService;
    private final IPlayerVisibilityService visibilityService;
    private final IMenuService menuService;

    @Inject
    public HotbarListener(IHotbarService hotbarService, IPlayerVisibilityService visibilityService,
            IMenuService menuService) {
        this.hotbarService = hotbarService;
        this.visibilityService = visibilityService;
        this.menuService = menuService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        hotbarService.giveLobbyHotbar(player);
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
            menuService.openQuickplayMenu(player);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME))) {
            event.setCancelled(true);
            menuService.openYourProfileMenu(player);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DISPLAY_NAME))) {
            event.setCancelled(true);
            menuService.openBedWarsMenu(player);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.SHOP.DISPLAY_NAME))) {
            event.setCancelled(true);
            CosmeticsAPI.get().getShopById("main").open(player);
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DISPLAY_NAME))) {
            event.setCancelled(true);
            PlayerManager playerManager = GadgetsMenuAPI.getPlayerManager(player);
            playerManager.goBackToMainMenu();
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DISPLAY_NAME))
                ||
                displayName.equals(
                        ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DISPLAY_NAME))) {
            event.setCancelled(true);
            if (!visibilityService.toggleVisibilityWithCooldown(player)) {
                long remaining = visibilityService.getRemainingCooldown(player);
                String seconds = formatSeconds(remaining);
                sendHotbarMessage(player, LanguageConfiguration.HOTBAR.VISIBILITY_COOLDOWN
                        .replace("%seconds%", seconds));
                return;
            }

            if (visibilityService.isHidden(player)) {
                sendHotbarMessage(player, LanguageConfiguration.HOTBAR.VISIBILITY_HIDDEN);
            } else {
                sendHotbarMessage(player, LanguageConfiguration.HOTBAR.VISIBILITY_VISIBLE);
            }
            return;
        }
        if (displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DISPLAY_NAME))) {
            event.setCancelled(true);
            menuService.openLobbySelectorMenu(player);
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
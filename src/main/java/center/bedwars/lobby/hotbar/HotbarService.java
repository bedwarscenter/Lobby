package center.bedwars.lobby.hotbar;

import center.bedwars.lobby.configuration.configurations.ItemsConfiguration;
import center.bedwars.lobby.service.AbstractService;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.ItemBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import center.bedwars.lobby.visibility.IPlayerVisibilityService;

@Singleton
public class HotbarService extends AbstractService implements IHotbarService {

        private final IPlayerVisibilityService visibilityService;

        @Inject
        public HotbarService(IPlayerVisibilityService visibilityService) {
                this.visibilityService = visibilityService;
        }

        @Override
        protected void onEnable() {
        }

        @Override
        protected void onDisable() {
        }

        @Override
        public void giveLobbyHotbar(Player player) {
                clearHotbar(player);

                ItemStack quickPlay = new ItemBuilder(
                                Material.valueOf(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.MATERIAL))
                                .data((short) ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.SLOT, quickPlay);

                ItemStack profile = new ItemBuilder(Material.valueOf(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.MATERIAL))
                                .data((short) ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.LORE))
                                .build();
                if (ItemsConfiguration.LOBBY_HOTBAR.PROFILE.USE_PLAYER_SKULL) {
                        profile = new ItemBuilder(Material.SKULL_ITEM)
                                        .data((short) 3)
                                        .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME))
                                        .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.LORE))
                                        .skullOwner(player.getName())
                                        .build();
                }
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.SLOT, profile);

                ItemStack bedwarsMenu = new ItemBuilder(
                                Material.valueOf(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.MATERIAL))
                                .data((short) ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.SLOT, bedwarsMenu);

                ItemStack shop = new ItemBuilder(Material.valueOf(ItemsConfiguration.LOBBY_HOTBAR.SHOP.MATERIAL))
                                .data((short) ItemsConfiguration.LOBBY_HOTBAR.SHOP.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.SHOP.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.SHOP.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.SHOP.SLOT, shop);

                ItemStack collectibles = new ItemBuilder(
                                Material.valueOf(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.MATERIAL))
                                .data((short) ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.SLOT, collectibles);

                boolean isHidden = visibilityService.isHidden(player);
                ItemStack visibility;
                if (isHidden) {
                        visibility = new ItemBuilder(
                                        Material.valueOf(
                                                        ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.MATERIAL))
                                        .data((short) ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DATA)
                                        .name(ColorUtil.color(
                                                        ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DISPLAY_NAME))
                                        .lore(ColorUtil.colorList(
                                                        ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.LORE))
                                        .build();
                } else {
                        visibility = new ItemBuilder(
                                        Material.valueOf(
                                                        ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.MATERIAL))
                                        .data((short) ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DATA)
                                        .name(ColorUtil.color(
                                                        ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DISPLAY_NAME))
                                        .lore(ColorUtil.colorList(
                                                        ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.LORE))
                                        .build();
                }
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.SLOT, visibility);

                ItemStack lobbySelector = new ItemBuilder(
                                Material.valueOf(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.MATERIAL))
                                .data((short) ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.SLOT, lobbySelector);

                player.updateInventory();
        }

        @Override
        public void giveParkourHotbar(Player player) {
                clearHotbar(player);

                ItemStack reset = new ItemBuilder(Material.valueOf(ItemsConfiguration.PARKOUR_HOTBAR.RESET.MATERIAL))
                                .data((short) ItemsConfiguration.PARKOUR_HOTBAR.RESET.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.RESET.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.PARKOUR_HOTBAR.RESET.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.PARKOUR_HOTBAR.RESET.SLOT, reset);

                ItemStack checkpoint = new ItemBuilder(
                                Material.valueOf(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.MATERIAL))
                                .data((short) ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.SLOT, checkpoint);

                ItemStack exit = new ItemBuilder(Material.valueOf(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.MATERIAL))
                                .data((short) ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DATA)
                                .name(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME))
                                .lore(ColorUtil.colorList(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.LORE))
                                .build();
                player.getInventory().setItem(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.SLOT, exit);

                player.updateInventory();
        }

        @Override
        public void clearHotbar(Player player) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);
        }

        @Override
        public boolean isLobbyItem(ItemStack item) {
                if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                        return false;
                }
                String displayName = item.getItemMeta().getDisplayName();

                return displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DISPLAY_NAME)) ||
                                displayName.equals(
                                                ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME))
                                ||
                                displayName.equals(ColorUtil
                                                .color(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DISPLAY_NAME))
                                ||
                                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.SHOP.DISPLAY_NAME))
                                ||
                                displayName.equals(ColorUtil
                                                .color(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DISPLAY_NAME))
                                ||
                                displayName.equals(ColorUtil.color(
                                                ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DISPLAY_NAME))
                                ||
                                displayName.equals(ColorUtil.color(
                                                ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DISPLAY_NAME))
                                ||
                                displayName.equals(ColorUtil
                                                .color(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DISPLAY_NAME));
        }

        @Override
        public boolean isParkourItem(ItemStack item) {
                if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                        return false;
                }
                String displayName = item.getItemMeta().getDisplayName();

                return displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.RESET.DISPLAY_NAME)) ||
                                displayName.equals(ColorUtil
                                                .color(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME))
                                ||
                                displayName.equals(
                                                ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME));
        }

        @Override
        public void updateHotbar(Player player) {
                giveLobbyHotbar(player);
        }
}

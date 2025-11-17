package center.bedwars.lobby.manager.orphans;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.ItemsConfiguration;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.stream.Collectors;

public class HotbarManager extends Manager {

    private ParkourManager parkourManager;
    private PlayerVisibilityManager visibilityManager;

    @Override
    protected void onLoad() {
        ensureManagers();
    }

    @Override
    protected void onUnload() {
    }

    @Override
    protected void onFinish() {
        ensureManagers();
    }

    private void ensureManagers() {
        if (this.parkourManager == null) {
            this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
        }
        if (this.visibilityManager == null) {
            this.visibilityManager = Lobby.getManagerStorage().getManager(PlayerVisibilityManager.class);
        }
    }

    public void giveLobbyHotbar(Player player) {
        ensureManagers();
        player.getInventory().clear();

        boolean isHidden = visibilityManager != null && visibilityManager.isHidden(player);

        player.getInventory().setItem(
                ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.SLOT,
                createItem(
                        ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.MATERIAL,
                        ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DATA,
                        ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DISPLAY_NAME,
                        ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.LORE,
                        null
                )
        );

        player.getInventory().setItem(
                ItemsConfiguration.LOBBY_HOTBAR.PROFILE.SLOT,
                createItem(
                        ItemsConfiguration.LOBBY_HOTBAR.PROFILE.MATERIAL,
                        ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DATA,
                        ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME,
                        ItemsConfiguration.LOBBY_HOTBAR.PROFILE.LORE,
                        ItemsConfiguration.LOBBY_HOTBAR.PROFILE.USE_PLAYER_SKULL ? player.getName() : null
                )
        );

        player.getInventory().setItem(
                ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.SLOT,
                createItem(
                        ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.MATERIAL,
                        ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DATA,
                        ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DISPLAY_NAME,
                        ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.LORE,
                        null
                )
        );

        player.getInventory().setItem(
                ItemsConfiguration.LOBBY_HOTBAR.SHOP.SLOT,
                createItem(
                        ItemsConfiguration.LOBBY_HOTBAR.SHOP.MATERIAL,
                        ItemsConfiguration.LOBBY_HOTBAR.SHOP.DATA,
                        ItemsConfiguration.LOBBY_HOTBAR.SHOP.DISPLAY_NAME,
                        ItemsConfiguration.LOBBY_HOTBAR.SHOP.LORE,
                        null
                )
        );

        player.getInventory().setItem(
                ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.SLOT,
                createItem(
                        ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.MATERIAL,
                        ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DATA,
                        ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DISPLAY_NAME,
                        ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.LORE,
                        null
                )
        );

        if (isHidden) {
            player.getInventory().setItem(
                    ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.SLOT,
                    createItem(
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.MATERIAL,
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DATA,
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DISPLAY_NAME,
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.LORE,
                            null
                    )
            );
        } else {
            player.getInventory().setItem(
                    ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.SLOT,
                    createItem(
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.MATERIAL,
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DATA,
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DISPLAY_NAME,
                            ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.LORE,
                            null
                    )
            );
        }

        player.getInventory().setItem(
                ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.SLOT,
                createItem(
                        ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.MATERIAL,
                        ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DATA,
                        ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DISPLAY_NAME,
                        ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.LORE,
                        null
                )
        );

        player.updateInventory();
    }

    public void giveParkourHotbar(Player player) {
        ensureManagers();
        player.getInventory().clear();

        player.getInventory().setItem(
                ItemsConfiguration.PARKOUR_HOTBAR.RESET.SLOT,
                createItem(
                        ItemsConfiguration.PARKOUR_HOTBAR.RESET.MATERIAL,
                        ItemsConfiguration.PARKOUR_HOTBAR.RESET.DATA,
                        ItemsConfiguration.PARKOUR_HOTBAR.RESET.DISPLAY_NAME,
                        ItemsConfiguration.PARKOUR_HOTBAR.RESET.LORE,
                        null
                )
        );

        player.getInventory().setItem(
                ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.SLOT,
                createItem(
                        ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.MATERIAL,
                        ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DATA,
                        ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME,
                        ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.LORE,
                        null
                )
        );

        player.getInventory().setItem(
                ItemsConfiguration.PARKOUR_HOTBAR.EXIT.SLOT,
                createItem(
                        ItemsConfiguration.PARKOUR_HOTBAR.EXIT.MATERIAL,
                        ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DATA,
                        ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME,
                        ItemsConfiguration.PARKOUR_HOTBAR.EXIT.LORE,
                        null
                )
        );

        player.updateInventory();
    }

    public void updateHotbar(Player player) {
        ensureManagers();
        if (parkourManager == null) {
            giveLobbyHotbar(player);
            return;
        }

        ParkourSession session = parkourManager.getSessionManager().getSession(player);

        if (session != null) {
            giveParkourHotbar(player);
        } else {
            giveLobbyHotbar(player);
        }
    }

    private ItemStack createItem(String materialName, int data, String displayName, List<String> lore, String skullOwner) {
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(displayName));

            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(ColorUtil::color)
                        .collect(Collectors.toList()));
            }

            if (skullOwner != null && meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwner(skullOwner);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();

        return displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.QUICK_PLAY.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PROFILE.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.BEDWARS_MENU.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.SHOP.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.COLLECTIBLES.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.VISIBLE.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.PLAYER_VISIBILITY.HIDDEN.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.LOBBY_HOTBAR.LOBBY_SELECTOR.DISPLAY_NAME));
    }

    public boolean isParkourItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();

        return displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.RESET.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME)) ||
                displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME));
    }
}
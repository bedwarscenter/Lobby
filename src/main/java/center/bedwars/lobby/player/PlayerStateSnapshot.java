package center.bedwars.lobby.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record PlayerStateSnapshot(ItemStack[] contents, ItemStack[] armor, boolean allowFlight, boolean flying,
                                  GameMode gameMode, Location location) {

    public static PlayerStateSnapshot capture(Player player) {
        ItemStack[] contentsCopy = player.getInventory().getContents().clone();
        ItemStack[] armorCopy = player.getInventory().getArmorContents().clone();
        boolean allowFlight = player.getAllowFlight();
        boolean flying = player.isFlying();
        GameMode gameMode = player.getGameMode();
        Location location = player.getLocation().clone();
        return new PlayerStateSnapshot(contentsCopy, armorCopy, allowFlight, flying, gameMode, location);
    }

    public void restore(Player player, boolean restoreLocation) {
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        if (gameMode != null) {
            player.setGameMode(gameMode);
        }
        player.setAllowFlight(allowFlight);
        if (allowFlight) {
            player.setFlying(flying);
        }
        if (restoreLocation && location != null) {
            player.teleport(location);
        }
    }
}


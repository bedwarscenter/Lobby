package center.bedwars.lobby.listener.listeners.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.model.Parkour;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    private final ParkourManager parkourManager;

    public MoveListener() {
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        Location feetLocation = player.getLocation();
        Block blockAtFeet = feetLocation.getBlock();

        if (blockAtFeet.getType() == Material.WOOD_PLATE) {
            Block blockBelow = blockAtFeet.getRelative(0, -1, 0);
            handleParkourBlock(player, blockBelow);
            return;
        }

        Block blockBelow = feetLocation.subtract(0, 1, 0).getBlock();
        if (blockBelow.getType() == Material.WOOD_PLATE) {
            Block blockAbove = blockBelow.getRelative(0, -1, 0);
            handleParkourBlock(player, blockAbove);
        }
    }

    private void handleParkourBlock(Player player, Block block) {
        Material type = block.getType();
        Location location = block.getLocation();

        if (type == Material.GOLD_BLOCK) {
            Parkour parkour = parkourManager.getParkourAtLocation(location);
            if (parkour != null) {
                parkourManager.startParkour(player, parkour);
            }
        } else if ( type == Material.IRON_BLOCK) {
            parkourManager.handleCheckpoint(player, location);
        } else if ( type == Material.DIAMOND_BLOCK) {
            parkourManager.handleFinish(player, location);
        }
    }
}
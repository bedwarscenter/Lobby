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
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = player.getLocation();
        Block blockBelow = location.subtract(0, 1, 0).getBlock();

        if (blockBelow.getType() != Material.WOOD_PLATE) {
            return;
        }

        Block blockBelowPlate = blockBelow.getRelative(0, -1, 0);
        Material belowType = blockBelowPlate.getType();

        if (belowType == Material.GOLD_BLOCK) {
            Parkour parkour = parkourManager.getParkourAtLocation(blockBelowPlate.getLocation());
            if (parkour != null) {
                parkourManager.startParkour(player, parkour);
            }
        } else if (belowType == Material.IRON_BLOCK) {
            parkourManager.handleCheckpoint(player, blockBelowPlate.getLocation());
        } else if (belowType == Material.DIAMOND_BLOCK) {
            parkourManager.handleFinish(player, blockBelowPlate.getLocation());
        }
    }
}
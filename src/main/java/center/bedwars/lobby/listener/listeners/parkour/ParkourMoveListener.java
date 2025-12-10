package center.bedwars.lobby.listener.listeners.parkour;

import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.parkour.model.Parkour;
import com.google.inject.Inject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ParkourMoveListener implements Listener {

    private final IParkourService parkourService;

    @Inject
    public ParkourMoveListener(IParkourService parkourService) {
        this.parkourService = parkourService;
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
            Parkour parkour = parkourService.getParkourAtLocation(location);
            if (parkour != null) {
                parkourService.startParkour(player, parkour);
            }
        } else if (type == Material.IRON_BLOCK) {
            parkourService.handleCheckpoint(player, location);
        } else if (type == Material.DIAMOND_BLOCK) {
            parkourService.handleFinish(player, location);
        }
    }
}

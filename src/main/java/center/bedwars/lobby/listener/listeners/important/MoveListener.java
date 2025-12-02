package center.bedwars.lobby.listener.listeners.important;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class MoveListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Material blockType = event.getTo().getBlock().getRelative(BlockFace.DOWN).getType();

        if (blockType == Material.SLIME_BLOCK) {

            Vector direction = new Vector(1, 0.3, 0).multiply(1.2);

            player.playSound(player.getLocation(), Sound.PISTON_EXTEND, 1f, 1f);

            player.setVelocity(direction);
        }
    }
}

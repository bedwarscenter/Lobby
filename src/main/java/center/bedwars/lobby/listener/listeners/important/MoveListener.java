package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.snow.ISnowService;
import com.google.inject.Inject;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveListener implements Listener {

    private final ISnowService snowService;
    private final Map<UUID, Long> jumpPadCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 500;

    @Inject
    public MoveListener(ISnowService snowService) {
        this.snowService = snowService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            if (snowService != null) {
                snowService.onPlayerMove(player);
            }
        }

        Material blockType = event.getTo().getBlock().getRelative(BlockFace.DOWN).getType();

        if (blockType == SettingsConfiguration.JUMP_PAD.BLOCK_TYPE) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            if (jumpPadCooldowns.containsKey(playerId)) {
                long lastUse = jumpPadCooldowns.get(playerId);
                if (currentTime - lastUse < COOLDOWN_MS) {
                    return;
                }
            }

            jumpPadCooldowns.put(playerId, currentTime);

            Vector direction = new Vector(
                    SettingsConfiguration.JUMP_PAD.VELOCITY_X,
                    SettingsConfiguration.JUMP_PAD.VELOCITY_Y,
                    SettingsConfiguration.JUMP_PAD.VELOCITY_Z)
                    .multiply(SettingsConfiguration.JUMP_PAD.VELOCITY_MULTIPLIER);

            try {
                Sound sound = Sound.valueOf(SettingsConfiguration.JUMP_PAD.SOUND);
                player.playSound(player.getLocation(), sound,
                        SettingsConfiguration.JUMP_PAD.SOUND_VOLUME,
                        SettingsConfiguration.JUMP_PAD.SOUND_PITCH);
            } catch (IllegalArgumentException ignored) {
            }

            player.setVelocity(direction);
        }
    }
}

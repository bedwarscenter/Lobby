package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.util.SpawnUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerEnvironmentListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyState(event.getPlayer());
        if (SettingsConfiguration.PLAYER.TELEPORT_ON_JOIN) {
            SpawnUtil.teleportToSpawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        applyState(event.getPlayer());
        if (SettingsConfiguration.PLAYER.TELEPORT_ON_RESPAWN) {
            SpawnUtil.teleportToSpawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!SettingsConfiguration.PLAYER.DISABLE_HUNGER) {
            return;
        }

        Player player = (Player) event.getEntity();
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(SettingsConfiguration.PLAYER.SATURATION);
    }

    private void applyState(Player player) {
        if (SettingsConfiguration.PLAYER.FORCE_ADVENTURE) {
            if (player.getGameMode() != GameMode.ADVENTURE) {
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
        if (SettingsConfiguration.PLAYER.DISABLE_HUNGER) {
            player.setFoodLevel(20);
            player.setSaturation(SettingsConfiguration.PLAYER.SATURATION);
        }
    }
}


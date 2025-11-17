package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.SpawnUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerSafetyListener implements Listener {

    private final ParkourManager parkourManager;

    public PlayerSafetyListener() {
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!SettingsConfiguration.PLAYER.DISABLE_FALL_DAMAGE) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVoidFall(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || from == null) {
            return;
        }

        boolean crossedVoid = from.getY() > SettingsConfiguration.PLAYER.VOID_Y && to.getY() <= SettingsConfiguration.PLAYER.VOID_Y;
        if (!crossedVoid) {
            return;
        }

        if (parkourManager != null) {
            ParkourSession session = parkourManager.getSessionManager().getSession(player);
            if (session != null) {
                parkourManager.teleportToCheckpoint(player);
                return;
            }
        }

        if (SettingsConfiguration.PLAYER.AUTO_TELEPORT_ON_VOID) {
            SpawnUtil.teleportToSpawn(player);
        }
    }
}


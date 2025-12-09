package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.SpawnUtil;
import com.google.inject.Inject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerSafetyListener implements Listener {

    private final IParkourService parkourService;

    @Inject
    public PlayerSafetyListener(IParkourService parkourService) {
        this.parkourService = parkourService;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onVoidFall(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || from == null) {
            return;
        }

        boolean crossedVoid = from.getY() > SettingsConfiguration.PLAYER.VOID_Y
                && to.getY() <= SettingsConfiguration.PLAYER.VOID_Y;
        if (!crossedVoid) {
            return;
        }

        if (parkourService != null) {
            ParkourSession session = parkourService.getSession(player);
            if (session != null) {
                parkourService.teleportToCheckpoint(player);
                return;
            }
        }

        if (SettingsConfiguration.PLAYER.AUTO_TELEPORT_ON_VOID) {
            SpawnUtil.teleportToSpawn(player);
        }
    }
}

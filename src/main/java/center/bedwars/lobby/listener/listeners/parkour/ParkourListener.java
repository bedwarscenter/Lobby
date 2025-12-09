package center.bedwars.lobby.listener.listeners.parkour;

import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.ColorUtil;
import com.google.inject.Inject;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class ParkourListener implements Listener {

    private final IParkourService parkourService;

    @Inject
    public ParkourListener(IParkourService parkourService) {
        this.parkourService = parkourService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        ParkourSession session = parkourService.getSession(player);
        if (session != null) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.FLIGHT_DISABLED);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR) {
            ParkourSession session = parkourService.getSession(player);
            if (session != null) {
                parkourService.quitParkour(player);
                ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.GAMEMODE_CHANGE);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!parkourService.hasActiveSession(player)) {
            return;
        }

        event.setCancelled(true);
        parkourService.handleItemClick(player, event.getItem());
    }
}
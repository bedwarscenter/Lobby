package center.bedwars.lobby.listener.listeners.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.ColorUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class ParkourListener implements Listener {

    private final ParkourManager parkourManager;

    public ParkourListener() {
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        parkourManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlightToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!event.isFlying() || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ParkourSession session = parkourManager.getSessionManager().getSession(player);
        if (session != null) {
            event.setCancelled(true);
            parkourManager.quitParkour(player);
            ColorUtil.sendMessage(player, "&cYou cannot fly during parkour!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR) {
            ParkourSession session = parkourManager.getSessionManager().getSession(player);
            if (session != null) {
                parkourManager.quitParkour(player);
                ColorUtil.sendMessage(player, "&cParkour ended due to gamemode change!");
            }
        }
    }
}
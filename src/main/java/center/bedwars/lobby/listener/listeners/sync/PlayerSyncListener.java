package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.sync.IPlayerSyncService;
import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSyncListener implements Listener {

    private final IPlayerSyncService playerSyncService;

    @Inject
    public PlayerSyncListener(IPlayerSyncService playerSyncService) {
        this.playerSyncService = playerSyncService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerSyncService.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerSyncService.handlePlayerQuit(event.getPlayer());
    }
}

package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.PlayerSyncManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSyncListener implements Listener {

    private final PlayerSyncManager playerSyncManager;

    public PlayerSyncListener() {
        this.playerSyncManager = Lobby.getManagerStorage().getManager(PlayerSyncManager.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerSyncManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerSyncManager.handlePlayerQuit(event.getPlayer());
    }
}
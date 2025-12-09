package center.bedwars.lobby.listener.listeners.snow;

import center.bedwars.lobby.snow.ISnowService;
import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SnowListener implements Listener {

    private final ISnowService snowService;

    @Inject
    public SnowListener(ISnowService snowService) {
        this.snowService = snowService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        snowService.onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        snowService.onPlayerQuit(event.getPlayer());
    }
}

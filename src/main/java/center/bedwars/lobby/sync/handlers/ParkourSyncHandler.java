package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.sync.SyncEvent;
import com.google.inject.Inject;
import org.bukkit.Bukkit;

public class ParkourSyncHandler implements ISyncHandler {

    private final Lobby plugin;
    private final IParkourService parkourService;

    @Inject
    public ParkourSyncHandler(Lobby plugin, IParkourService parkourService) {
        this.plugin = plugin;
        this.parkourService = parkourService;
    }

    @Override
    public void handle(SyncEvent event) {
        Bukkit.getScheduler().runTask(plugin, parkourService::refreshParkours);
    }
}
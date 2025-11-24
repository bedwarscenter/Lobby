package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.SyncEvent;
import org.bukkit.Bukkit;

public class ParkourSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                Lobby.getManagerStorage().getManager(ParkourManager.class).refreshParkours());
    }
}
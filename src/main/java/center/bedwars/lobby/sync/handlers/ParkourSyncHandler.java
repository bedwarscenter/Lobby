package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.SyncEvent;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

public class ParkourSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();

        if (!data.has("action")) {
            return;
        }

        String action = data.get("action").getAsString();

        ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            if (action.equals("refresh")) {
                parkourManager.refreshParkours();
            }
        });
    }
}
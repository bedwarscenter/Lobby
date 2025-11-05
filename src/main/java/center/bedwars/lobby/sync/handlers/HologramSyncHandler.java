package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.DecentHologramsDependency;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.Arrays;

public class HologramSyncHandler implements ISyncHandler {

    public HologramSyncHandler() {
        DependencyManager depManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        DecentHologramsDependency decentHolograms = depManager.getDecentHolograms();

        if (!decentHolograms.isApiAvailable()) {
            throw new IllegalStateException("DecentHolograms API is not available!");
        }
    }

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        String hologramId = data.get("hologramId").getAsString();

        switch (event.getType()) {
            case HOLOGRAM_CREATE:
                handleCreate(data, hologramId);
                break;
            case HOLOGRAM_DELETE:
                handleDelete(hologramId);
                break;
            case HOLOGRAM_UPDATE:
                handleUpdate(data, hologramId);
                break;
        }
    }

    private void handleCreate(JsonObject data, String hologramId) {
        Location loc = SyncDataSerializer.deserializeLocation(
                data.getAsJsonObject("location"),
                Lobby.getINSTANCE().getServer()
        );

        String[] lines = SyncDataSerializer.deserializeHologramLines(data);

        Hologram hologram = DHAPI.createHologram(hologramId, loc);
        DHAPI.setHologramLines(hologram, Arrays.asList(lines));
    }

    private void handleDelete(String hologramId) {
        Hologram hologram = DHAPI.getHologram(hologramId);
        if (hologram != null) {
            DHAPI.removeHologram(hologramId);
        }
    }

    private void handleUpdate(JsonObject data, String hologramId) {
        Hologram hologram = DHAPI.getHologram(hologramId);
        if (hologram == null) return;

        if (data.has("location")) {
            Location loc = SyncDataSerializer.deserializeLocation(
                    data.getAsJsonObject("location"),
                    Lobby.getINSTANCE().getServer()
            );
            DHAPI.moveHologram(hologram, loc);
        }

        if (data.has("lines")) {
            String[] lines = SyncDataSerializer.deserializeHologramLines(data);
            DHAPI.setHologramLines(hologram, Arrays.asList(lines));
        }
    }
}
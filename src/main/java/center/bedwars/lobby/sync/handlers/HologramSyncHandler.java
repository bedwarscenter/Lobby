package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.Arrays;

public class HologramSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        String id = data.get("id").getAsString();

        switch (event.getType()) {
            case HOLOGRAM_CREATE -> handleCreate(data, id);
            case HOLOGRAM_DELETE -> handleDelete(id);
            case HOLOGRAM_UPDATE -> handleUpdate(data, id);
        }
    }

    private void handleCreate(JsonObject data, String id) {
        Location loc = SyncDataSerializer.deserializeLocation(
                data.getAsJsonObject("loc"), Lobby.getINSTANCE().getServer());
        Hologram hologram = DHAPI.createHologram(id, loc);
        DHAPI.setHologramLines(hologram, Arrays.asList(SyncDataSerializer.deserializeHologramLines(data)));
    }

    private void handleDelete(String id) {
        if (DHAPI.getHologram(id) != null) {
            DHAPI.removeHologram(id);
        }
    }

    private void handleUpdate(JsonObject data, String id) {
        Hologram hologram = DHAPI.getHologram(id);
        if (hologram == null) return;

        if (data.has("loc")) {
            DHAPI.moveHologram(hologram, SyncDataSerializer.deserializeLocation(
                    data.getAsJsonObject("loc"), Lobby.getINSTANCE().getServer()));
        }

        if (data.has("lines")) {
            DHAPI.setHologramLines(hologram, Arrays.asList(SyncDataSerializer.deserializeHologramLines(data)));
        }
    }
}
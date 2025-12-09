package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.Serializer;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Arrays;

public class HologramSyncHandler implements ISyncHandler {

    private final Lobby plugin;

    @com.google.inject.Inject
    public HologramSyncHandler(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(SyncEvent event) {
        try {
            Serializer.HologramData hologramData = Serializer.deserialize(event.getData(),
                    Serializer.HologramData.class);

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (event.getType() == SyncEventType.HOLOGRAM_CREATE) {
                        handleCreate(hologramData);
                    } else if (event.getType() == SyncEventType.HOLOGRAM_DELETE) {
                        handleDelete(hologramData.hologramId);
                    } else if (event.getType() == SyncEventType.HOLOGRAM_UPDATE) {
                        handleUpdate(hologramData);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to sync hologram: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCreate(Serializer.HologramData data) {
        Hologram existing = DHAPI.getHologram(data.hologramId);
        if (existing != null) {
            DHAPI.removeHologram(data.hologramId);
        }

        Location loc = data.location.toLocation(Bukkit.getServer());
        if (loc == null)
            return;

        Hologram hologram = DHAPI.createHologram(data.hologramId, loc);
        DHAPI.setHologramLines(hologram, Arrays.asList(data.lines));
    }

    private void handleDelete(String id) {
        Hologram hologram = DHAPI.getHologram(id);
        if (hologram != null) {
            DHAPI.removeHologram(id);
        }
    }

    private void handleUpdate(Serializer.HologramData data) {
        Hologram hologram = DHAPI.getHologram(data.hologramId);
        if (hologram == null) {
            handleCreate(data);
            return;
        }

        Location loc = data.location.toLocation(Bukkit.getServer());
        if (loc != null) {
            DHAPI.moveHologram(hologram, loc);
        }

        DHAPI.setHologramLines(hologram, Arrays.asList(data.lines));
    }
}
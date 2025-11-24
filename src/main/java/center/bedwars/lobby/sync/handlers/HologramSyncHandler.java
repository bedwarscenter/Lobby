package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.netty.buffer.ByteBuf;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Arrays;

public class HologramSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        ByteBuf data = event.getData();
        String id = SyncDataSerializer.readUTF(data);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            try {
                if (event.getType() == SyncEventType.HOLOGRAM_CREATE) {
                    handleCreate(data, id);
                } else if (event.getType() == SyncEventType.HOLOGRAM_DELETE) {
                    handleDelete(id);
                } else if (event.getType() == SyncEventType.HOLOGRAM_UPDATE) {
                    handleUpdate(data, id);
                }
            } catch (Exception e) {
                Lobby.getINSTANCE().getLogger().warning("Failed to sync hologram: " + e.getMessage());
            }
        });
    }

    private void handleCreate(ByteBuf data, String id) {
        Hologram existing = DHAPI.getHologram(id);
        if (existing != null) {
            DHAPI.removeHologram(id);
        }
        Location loc = SyncDataSerializer.deserializeLocation(data, Lobby.getINSTANCE().getServer());
        if (loc == null) return;
        Hologram hologram = DHAPI.createHologram(id, loc);
        String[] lines = SyncDataSerializer.deserializeHologramLines(data);
        DHAPI.setHologramLines(hologram, Arrays.asList(lines));
    }

    private void handleDelete(String id) {
        Hologram hologram = DHAPI.getHologram(id);
        if (hologram != null) {
            DHAPI.removeHologram(id);
        }
    }

    private void handleUpdate(ByteBuf data, String id) {
        Hologram hologram = DHAPI.getHologram(id);
        if (hologram == null) {
            handleCreate(data, id);
            return;
        }
        if (data.readableBytes() > 0) {
            Location loc = SyncDataSerializer.deserializeLocation(data, Lobby.getINSTANCE().getServer());
            if (loc != null) {
                DHAPI.moveHologram(hologram, loc);
            }
        }
        String[] lines = SyncDataSerializer.deserializeHologramLines(data);
        DHAPI.setHologramLines(hologram, Arrays.asList(lines));
    }
}
package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

public class BlockSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        Location loc = SyncDataSerializer.deserializeLocation(
                data.getAsJsonObject("loc"), Lobby.getINSTANCE().getServer());

        if (loc.getWorld() == null) return;

        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load(true);
        }

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            try {
                if (event.getType() == SyncEventType.BLOCK_PLACE) {
                    loc.getBlock().setType(Material.valueOf(data.get("mat").getAsString()));
                    loc.getBlock().setData(data.get("d").getAsByte());
                } else {
                    loc.getBlock().setType(Material.AIR);
                }
            } catch (Exception ignored) {}
        });
    }
}
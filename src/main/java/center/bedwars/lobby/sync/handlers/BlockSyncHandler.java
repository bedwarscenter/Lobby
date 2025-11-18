package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        Location loc = SyncDataSerializer.deserializeLocation(
                data.getAsJsonObject("location"),
                Lobby.getINSTANCE().getServer()
        );

        if (loc.getWorld() == null) {
            Lobby.getINSTANCE().getLogger().warning("World is null for block sync");
            return;
        }

        Chunk chunk = loc.getChunk();
        if (!chunk.isLoaded()) {
            Lobby.getINSTANCE().getLogger().info("Loading chunk for block sync: " + chunk.getX() + ", " + chunk.getZ());
            chunk.load(true);
        }

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            try {
                Block block = loc.getBlock();

                if (event.getType() == SyncEventType.BLOCK_PLACE) {
                    Material material = Material.valueOf(data.get("material").getAsString());
                    byte blockData = data.get("data").getAsByte();

                    block.setType(material);
                    block.setData(blockData);

                    Lobby.getINSTANCE().getLogger().info("Placed block: " + material + " at " + loc);

                } else if (event.getType() == SyncEventType.BLOCK_BREAK) {
                    Material oldMaterial = block.getType();
                    block.setType(Material.AIR);

                    Lobby.getINSTANCE().getLogger().info("Broke block: " + oldMaterial + " at " + loc);
                }

            } catch (Exception e) {
                Lobby.getINSTANCE().getLogger().severe("Error handling block sync: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
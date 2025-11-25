package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.KryoSerializer;
import center.bedwars.lobby.sync.serialization.KryoSerializer.BlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        try {
            BlockData blockData = KryoSerializer.deserialize(event.getData(), BlockData.class);
            Location loc = blockData.location.toLocation(Lobby.getINSTANCE().getServer());

            if (loc == null || loc.getWorld() == null) return;
            if (!loc.getChunk().isLoaded()) loc.getChunk().load(true);

            Material material = Material.valueOf(blockData.material);
            byte data = blockData.data;

            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                Block block = loc.getBlock();
                if (event.getType() == SyncEventType.BLOCK_PLACE) {
                    block.setType(material);
                    block.setData(data);
                } else if (event.getType() == SyncEventType.BLOCK_BREAK) {
                    block.setType(Material.AIR);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
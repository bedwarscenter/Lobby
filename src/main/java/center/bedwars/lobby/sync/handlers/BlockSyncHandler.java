package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.Serializer;
import org.bukkit.Bukkit;

public class BlockSyncHandler implements ISyncHandler {

    private final Lobby plugin;

    @com.google.inject.Inject
    public BlockSyncHandler(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(SyncEvent event) {
        try {
            Serializer.BlockData blockData = Serializer.deserialize(event.getData(), Serializer.BlockData.class);

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    org.bukkit.Location loc = blockData.location.toLocation(Bukkit.getServer());
                    if (loc == null || loc.getWorld() == null)
                        return;

                    if (!loc.getChunk().isLoaded()) {
                        loc.getChunk().load(true);
                    }

                    org.bukkit.block.Block block = loc.getBlock();

                    if (event.getType() == SyncEventType.BLOCK_PLACE) {
                        block.setType(blockData.getMaterial());
                        block.setData(blockData.data);
                    } else if (event.getType() == SyncEventType.BLOCK_BREAK) {
                        block.setType(org.bukkit.Material.AIR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
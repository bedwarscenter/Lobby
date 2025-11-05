package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.LobbySyncManager;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class LobbySyncListener implements Listener {

    private final LobbySyncManager syncManager;

    public LobbySyncListener() {
        this.syncManager = Lobby
                .getManagerStorage()
                .getManager(LobbySyncManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        byte data = block.getData();

        JsonObject eventData = SyncDataSerializer.serializeBlockData(
                block.getLocation(),
                material,
                data
        );

        syncManager.broadcastEvent(SyncEventType.BLOCK_PLACE, eventData);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();

        JsonObject eventData = SyncDataSerializer.serializeBlockData(
                block.getLocation(),
                material,
                (byte) 0
        );

        syncManager.broadcastEvent(SyncEventType.BLOCK_BREAK, eventData);
    }
}
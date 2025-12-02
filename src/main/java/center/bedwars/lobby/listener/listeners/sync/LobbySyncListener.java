package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.LobbySyncManager;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.Serializer;
import center.bedwars.lobby.sync.serialization.Serializer.BlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class LobbySyncListener implements Listener {

    private final LobbySyncManager syncManager;

    public LobbySyncListener() {
        this.syncManager = Lobby.getManagerStorage().getManager(LobbySyncManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("SyncMode")) return;

        Block block = event.getBlock();
        BlockData blockData = new BlockData(block.getLocation(), block.getType(), block.getData());
        byte[] serialized = Serializer.serialize(blockData);
        syncManager.broadcastEvent(SyncEventType.BLOCK_PLACE, serialized);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("SyncMode")) return;

        Block block = event.getBlock();
        BlockData blockData = new BlockData(block.getLocation(), Material.AIR, (byte) 0);
        byte[] serialized = Serializer.serialize(blockData);
        syncManager.broadcastEvent(SyncEventType.BLOCK_BREAK, serialized);
    }
}
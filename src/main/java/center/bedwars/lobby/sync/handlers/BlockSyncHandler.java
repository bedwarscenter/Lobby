package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import io.netty.buffer.ByteBuf;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        ByteBuf data = event.getData();
        Location loc = SyncDataSerializer.deserializeLocation(data, Lobby.getINSTANCE().getServer());
        if (loc == null || loc.getWorld() == null) return;
        if (!loc.getChunk().isLoaded()) loc.getChunk().load(true);

        Material material = Material.valueOf(SyncDataSerializer.readUTF(data));
        byte blockData = data.readByte();

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            Block block = loc.getBlock();
            if (event.getType() == SyncEventType.BLOCK_PLACE) {
                block.setType(material);
                block.setData(blockData);
            } else if (event.getType() == SyncEventType.BLOCK_BREAK) {
                block.setType(Material.AIR);
            }
        });
    }
}
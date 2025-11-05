package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
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
            return;
        }

        Block block = loc.getBlock();

        if (event.getType() == SyncEventType.BLOCK_PLACE) {
            Material material = Material.valueOf(data.get("material").getAsString());
            byte blockData = data.get("data").getAsByte();

            block.setType(material);
            block.setData(blockData);

        } else if (event.getType() == SyncEventType.BLOCK_BREAK) {
            block.setType(Material.AIR);
        }
    }
}
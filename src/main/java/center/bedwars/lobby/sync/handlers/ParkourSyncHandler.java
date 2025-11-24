package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import io.netty.buffer.ByteBuf;
import org.bukkit.Bukkit;

public class ParkourSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        ByteBuf data = event.getData();
        if (data.readableBytes() > 0 && "refresh".equals(SyncDataSerializer.readUTF(data))) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                    Lobby.getManagerStorage().getManager(ParkourManager.class).refreshParkours());
        }
    }
}
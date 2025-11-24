package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import io.netty.buffer.ByteBuf;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class WorldSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        ByteBuf data = event.getData();
        String worldName = SyncDataSerializer.readUTF(data);
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            if (data.readableBytes() > 0) {
                applyBorder(world, data);
            }
            if (data.readableBytes() > 0) {
                int len = data.readInt();
                for (int i = 0; i < len; i++) {
                    String key = SyncDataSerializer.readUTF(data);
                    String value = SyncDataSerializer.readUTF(data);
                    world.setGameRuleValue(key, value);
                }
            }
            if (data.readableBytes() > 0) {
                Difficulty difficulty = Difficulty.valueOf(SyncDataSerializer.readUTF(data));
                world.setDifficulty(difficulty);
            }
            if (data.readableBytes() > 0) {
                world.setPVP(data.readBoolean());
            }
            if (data.readableBytes() > 0) {
                world.setTime(data.readLong());
            }
            if (data.readableBytes() > 0) {
                world.setStorm(data.readBoolean());
            }
            if (data.readableBytes() > 0) {
                world.setThundering(data.readBoolean());
            }
        });
    }

    private void applyBorder(World world, ByteBuf data) {
        WorldBorder border = world.getWorldBorder();
        if (data.readableBytes() > 0) {
            double x = data.readDouble();
            double z = data.readDouble();
            border.setCenter(x, z);
        }
        if (data.readableBytes() > 0) {
            border.setSize(data.readDouble());
        }
        if (data.readableBytes() > 0) {
            border.setDamageAmount(data.readDouble());
        }
        if (data.readableBytes() > 0) {
            border.setDamageBuffer(data.readDouble());
        }
        if (data.readableBytes() > 0) {
            border.setWarningDistance(data.readInt());
        }
        if (data.readableBytes() > 0) {
            border.setWarningTime(data.readInt());
        }
    }
}
package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.KryoSerializer;
import center.bedwars.lobby.sync.serialization.KryoSerializer.WorldSyncData;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class WorldSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        try {
            WorldSyncData data = KryoSerializer.deserialize(event.getData(), WorldSyncData.class);
            World world = Bukkit.getWorld(data.worldName);
            if (world == null) return;

            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                if (data.border != null) {
                    applyBorder(world, data.border);
                }

                if (data.gameRules != null) {
                    for (java.util.Map.Entry<String, String> entry : data.gameRules.entrySet()) {
                        world.setGameRuleValue(entry.getKey(), entry.getValue());
                    }
                }

                if (data.difficulty != null) {
                    world.setDifficulty(Difficulty.valueOf(data.difficulty));
                }

                world.setPVP(data.pvp);
                world.setTime(data.time);
                world.setStorm(data.storm);
                world.setThundering(data.thundering);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyBorder(World world, center.bedwars.lobby.sync.serialization.KryoSerializer.WorldBorderData data) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(data.centerX, data.centerZ);
        border.setSize(data.size);
        border.setDamageAmount(data.damageAmount);
        border.setDamageBuffer(data.damageBuffer);
        border.setWarningDistance(data.warningDistance);
        border.setWarningTime(data.warningTime);
    }
}
package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class WorldSyncHandler implements ISyncHandler {

    private final Lobby plugin;

    @com.google.inject.Inject
    public WorldSyncHandler(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(SyncEvent event) {
        try {
            Serializer.WorldSyncData data = Serializer.deserialize(event.getData(), Serializer.WorldSyncData.class);
            World world = Bukkit.getWorld(data.worldName);
            if (world == null)
                return;

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (data.border != null) {
                        WorldBorder border = world.getWorldBorder();
                        border.setCenter(data.border.centerX, data.border.centerZ);
                        border.setSize(data.border.size);
                        border.setDamageAmount(data.border.damageAmount);
                        border.setDamageBuffer(data.border.damageBuffer);
                        border.setWarningDistance(data.border.warningDistance);
                        border.setWarningTime(data.border.warningTime);
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

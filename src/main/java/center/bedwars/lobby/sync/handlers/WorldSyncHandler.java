package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.Map;

public class WorldSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        String worldName = data.get("worldName").getAsString();

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            if (data.has("worldBorder")) {
                applyWorldBorder(world, data.getAsJsonObject("worldBorder"));
            }

            if (data.has("gameRules")) {
                applyGameRules(world, data.getAsJsonObject("gameRules"));
            }

            if (data.has("difficulty")) {
                world.setDifficulty(org.bukkit.Difficulty.valueOf(data.get("difficulty").getAsString()));
            }

            if (data.has("pvp")) {
                world.setPVP(data.get("pvp").getAsBoolean());
            }

            if (data.has("time")) {
                world.setTime(data.get("time").getAsLong());
            }

            if (data.has("storm")) {
                world.setStorm(data.get("storm").getAsBoolean());
            }

            if (data.has("thundering")) {
                world.setThundering(data.get("thundering").getAsBoolean());
            }

            if (data.has("weather")) {
                world.setWeatherDuration(data.get("weather").getAsInt());
            }
        });
    }

    private void applyWorldBorder(World world, JsonObject borderData) {
        WorldBorder border = world.getWorldBorder();

        if (borderData.has("center")) {
            JsonObject center = borderData.getAsJsonObject("center");
            border.setCenter(center.get("x").getAsDouble(), center.get("z").getAsDouble());
        }

        if (borderData.has("size")) {
            border.setSize(borderData.get("size").getAsDouble());
        }

        if (borderData.has("damageAmount")) {
            border.setDamageAmount(borderData.get("damageAmount").getAsDouble());
        }

        if (borderData.has("damageBuffer")) {
            border.setDamageBuffer(borderData.get("damageBuffer").getAsDouble());
        }

        if (borderData.has("warningDistance")) {
            border.setWarningDistance(borderData.get("warningDistance").getAsInt());
        }

        if (borderData.has("warningTime")) {
            border.setWarningTime(borderData.get("warningTime").getAsInt());
        }
    }

    private void applyGameRules(World world, JsonObject gameRules) {
        for (Map.Entry<String, JsonElement> entry : gameRules.entrySet()) {
            world.setGameRuleValue(entry.getKey(), entry.getValue().getAsString());
        }
    }
}
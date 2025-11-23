package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public class WorldSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        World world = Bukkit.getWorld(data.get("worldName").getAsString());
        if (world == null) return;

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            if (data.has("worldBorder")) {
                applyBorder(world, data.getAsJsonObject("worldBorder"));
            }
            if (data.has("gameRules")) {
                data.getAsJsonObject("gameRules").entrySet()
                        .forEach(e -> world.setGameRuleValue(e.getKey(), e.getValue().getAsString()));
            }
            if (data.has("difficulty")) {
                world.setDifficulty(org.bukkit.Difficulty.valueOf(data.get("difficulty").getAsString()));
            }
            if (data.has("pvp")) world.setPVP(data.get("pvp").getAsBoolean());
            if (data.has("time")) world.setTime(data.get("time").getAsLong());
            if (data.has("storm")) world.setStorm(data.get("storm").getAsBoolean());
            if (data.has("thundering")) world.setThundering(data.get("thundering").getAsBoolean());
        });
    }

    private void applyBorder(World world, JsonObject data) {
        WorldBorder border = world.getWorldBorder();
        if (data.has("center")) {
            JsonObject center = data.getAsJsonObject("center");
            border.setCenter(center.get("x").getAsDouble(), center.get("z").getAsDouble());
        }
        if (data.has("size")) border.setSize(data.get("size").getAsDouble());
        if (data.has("damageAmount")) border.setDamageAmount(data.get("damageAmount").getAsDouble());
        if (data.has("damageBuffer")) border.setDamageBuffer(data.get("damageBuffer").getAsDouble());
        if (data.has("warningDistance")) border.setWarningDistance(data.get("warningDistance").getAsInt());
        if (data.has("warningTime")) border.setWarningTime(data.get("warningTime").getAsInt());
    }
}
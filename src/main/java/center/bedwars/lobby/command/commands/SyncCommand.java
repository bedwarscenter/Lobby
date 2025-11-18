package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.CarbonDependency;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.LobbySyncManager;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.handlers.ChunkSnapshotSyncHandler;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import center.bedwars.lobby.util.ColorUtil;
import com.google.gson.JsonObject;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.command.Requires;
import net.j4c0b3y.api.command.annotation.parameter.Default;
import net.j4c0b3y.api.command.annotation.parameter.Named;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.parameter.modifier.Range;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;
import xyz.refinedev.spigot.features.chunk.snapshot.ICarbonChunkSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPOutputStream;

@Register(name = "sync")
@Requires("bedwarslobby.command.sync")
public class SyncCommand {

    private final Lobby lobby = Lobby.getINSTANCE();
    private final IChunkAPI chunkAPI;

    public SyncCommand() {
        DependencyManager depManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        CarbonDependency carbon = depManager.getCarbon();

        if (!carbon.isApiAvailable()) {
            throw new IllegalStateException("Carbon API is required for sync commands!");
        }

        this.chunkAPI = carbon.getChunkRegistry();
    }

    private LobbySyncManager getSyncManager() {
        return Lobby.getManagerStorage().getManager(LobbySyncManager.class);
    }

    @Command(name = "")
    public void main(@Sender Player sender) {
        ColorUtil.sendMessage(sender, "&8&m--------------------");
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.TITLE);
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.CONFIG_HELP);
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.CHUNK_HELP);
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.AREA_HELP);
        ColorUtil.sendMessage(sender, "&e/sync world &7- Sync world settings");
        ColorUtil.sendMessage(sender, "&e/sync parkour &7- Sync parkour courses");
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.FULL_HELP);
        ColorUtil.sendMessage(sender, "&8&m--------------------");
    }

    @Command(name = "config")
    public void configPush(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.CONFIG_PUSHING);

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            long reloadTime = ConfigurationManager.reloadConfigurations();

            JsonObject data = new JsonObject();
            data.addProperty("configType", "all");
            data.addProperty("reloadTime", reloadTime);

            getSyncManager().broadcastEvent(SyncEventType.CONFIG_PUSH, data);

            Bukkit.getScheduler().runTask(lobby, () ->
                    ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.CONFIG_PUSHED
                            .replace("%time%", String.valueOf(reloadTime)))
            );
        });
    }

    @Command(name = "chunk")
    public void chunkSync(@Sender Player player) {
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.CHUNK_SYNCING);

        Chunk chunk = player.getLocation().getChunk();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            try {
                byte[] snapshotData = serializeChunk(chunk);

                JsonObject data = SyncDataSerializer.serializeChunkSnapshot(chunkX, chunkZ, snapshotData);
                getSyncManager().broadcastEvent(SyncEventType.CHUNK_SNAPSHOT, data);

                Bukkit.getScheduler().runTask(lobby, () ->
                        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.CHUNK_SYNCED
                                .replace("%x%", String.valueOf(chunkX))
                                .replace("%z%", String.valueOf(chunkZ))
                                .replace("%size%", String.format("%.2f", snapshotData.length / 1024.0)))
                );
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(lobby, () ->
                        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.CHUNK_FAILED
                                .replace("%error%", e.getMessage()))
                );
                e.printStackTrace();
            }
        });
    }

    @Command(name = "area")
    public void areaSync(@Sender Player player, @Named("radius") @Default("5") @Range(min = 1, max = 20) int radius) {
        if (radius < 1 || radius > 20) {
            ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.RADIUS_ERROR);
            return;
        }

        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.AREA_SYNCING
                .replace("%radius%", String.valueOf(radius)));

        Chunk centerChunk = player.getLocation().getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        World world = centerChunk.getWorld();

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            int synced = 0;
            long totalSize = 0;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int chunkX = centerX + x;
                    int chunkZ = centerZ + z;

                    try {
                        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                        byte[] snapshotData = serializeChunk(chunk);

                        JsonObject data = SyncDataSerializer.serializeChunkSnapshot(chunkX, chunkZ, snapshotData);
                        getSyncManager().broadcastEvent(SyncEventType.CHUNK_SNAPSHOT, data);

                        synced++;
                        totalSize += snapshotData.length;
                    } catch (Exception e) {
                        lobby.getLogger().warning(String.format(
                                "Failed to sync chunk [%d, %d]: %s", chunkX, chunkZ, e.getMessage()
                        ));
                    }
                }
            }

            final int finalSynced = synced;
            final long finalSize = totalSize;

            Bukkit.getScheduler().runTask(lobby, () ->
                    ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.AREA_SYNCED
                            .replace("%chunks%", String.valueOf(finalSynced))
                            .replace("%size%", String.format("%.2f", finalSize / 1024.0)))
            );
        });
    }

    @Command(name = "world")
    public void worldSync(@Sender Player player) {
        ColorUtil.sendMessage(player, "&eStarting world synchronization...");

        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("worldName", world.getName());

                JsonObject borderData = new JsonObject();
                JsonObject center = new JsonObject();
                center.addProperty("x", border.getCenter().getX());
                center.addProperty("z", border.getCenter().getZ());
                borderData.add("center", center);
                borderData.addProperty("size", border.getSize());
                borderData.addProperty("damageAmount", border.getDamageAmount());
                borderData.addProperty("damageBuffer", border.getDamageBuffer());
                borderData.addProperty("warningDistance", border.getWarningDistance());
                borderData.addProperty("warningTime", border.getWarningTime());
                data.add("worldBorder", borderData);

                JsonObject gameRules = new JsonObject();
                for (String rule : world.getGameRules()) {
                    gameRules.addProperty(rule, world.getGameRuleValue(rule));
                }
                data.add("gameRules", gameRules);

                data.addProperty("difficulty", world.getDifficulty().name());
                data.addProperty("pvp", world.getPVP());
                data.addProperty("time", world.getTime());
                data.addProperty("storm", world.hasStorm());
                data.addProperty("thundering", world.isThundering());
                data.addProperty("weather", world.getWeatherDuration());

                getSyncManager().broadcastEvent(SyncEventType.WORLD_SYNC, data);

                Bukkit.getScheduler().runTask(lobby, () ->
                        ColorUtil.sendMessage(player, "&aWorld settings synchronized successfully!")
                );
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(lobby, () ->
                        ColorUtil.sendMessage(player, "&cFailed to sync world: " + e.getMessage())
                );
                e.printStackTrace();
            }
        });
    }

    @Command(name = "parkour")
    public void parkourSync(@Sender Player player) {
        ColorUtil.sendMessage(player, "&eStarting parkour synchronization...");

        ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);

        Bukkit.getScheduler().runTask(lobby, () -> {
            parkourManager.scheduleParkourRefresh();

            JsonObject data = new JsonObject();
            data.addProperty("action", "refresh");

            getSyncManager().broadcastEvent(SyncEventType.PARKOUR_SYNC, data);

            ColorUtil.sendMessage(player, "&aParkour courses synchronized successfully!");
        });
    }

    @Command(name = "full")
    public void fullSync(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.FULL_SYNCING);
        getSyncManager().performFullSync();
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.FULL_SENT);
    }

    private byte[] serializeChunk(Chunk chunk) throws Exception {
        ICarbonChunkSnapshot<?> snapshot = chunkAPI.takeSnapshot(chunk);
        return ChunkSnapshotSyncHandler.serializeSnapshot(snapshot);
    }
}
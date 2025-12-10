package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.IConfigurationService;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.sync.ILobbySyncService;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.handlers.ChunkSnapshotSyncHandler;
import center.bedwars.lobby.sync.serialization.Serializer;
import center.bedwars.api.util.ColorUtil;
import com.google.inject.Inject;
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
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Register(name = "sync")
@Requires("bedwarslobby.command.sync")
public class SyncCommand {

    private final Lobby lobby;
    private final ILobbySyncService syncService;
    private final IConfigurationService configService;
    private final IParkourService parkourService;

    @Inject
    public SyncCommand(Lobby lobby, ILobbySyncService syncService,
            IConfigurationService configService, IParkourService parkourService) {
        this.lobby = lobby;
        this.syncService = syncService;
        this.configService = configService;
        this.parkourService = parkourService;
    }

    @Command(name = "")
    public void sync(@Sender Player sender) {
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
            long reloadTime = configService.reloadConfigurations();
            syncService.broadcastEvent(SyncEventType.CONFIG_PUSH, new byte[0]);
            Bukkit.getScheduler().runTask(lobby,
                    () -> ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.CONFIG_PUSHED
                            .replace("%time%", String.valueOf(reloadTime))));
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
                if (!chunk.isLoaded()) {
                    Bukkit.getScheduler().runTask(lobby, () -> chunk.load(true));
                    Thread.sleep(100);
                }

                byte[] snapshotData = ChunkSnapshotSyncHandler.serialize(chunk);
                Serializer.ChunkData chunkData = new Serializer.ChunkData(chunkX, chunkZ, snapshotData);
                byte[] serialized = Serializer.serialize(chunkData);

                syncService.broadcastEvent(SyncEventType.CHUNK_SNAPSHOT, serialized);

                Bukkit.getScheduler().runTask(lobby,
                        () -> ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.CHUNK_SYNCED
                                .replace("%x%", String.valueOf(chunkX))
                                .replace("%z%", String.valueOf(chunkZ))
                                .replace("%size%", String.format("%.2f", snapshotData.length / 1024.0))));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(lobby,
                        () -> ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.CHUNK_FAILED
                                .replace("%error%", e.getMessage())));
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
                        if (!chunk.isLoaded()) {
                            Bukkit.getScheduler().runTask(lobby, () -> chunk.load(true));
                            Thread.sleep(50);
                        }

                        byte[] snapshotData = ChunkSnapshotSyncHandler.serialize(chunk);
                        Serializer.ChunkData chunkData = new Serializer.ChunkData(chunkX, chunkZ, snapshotData);
                        byte[] serialized = Serializer.serialize(chunkData);

                        syncService.broadcastEvent(SyncEventType.CHUNK_SNAPSHOT, serialized);

                        synced++;
                        totalSize += snapshotData.length;
                        Thread.sleep(10);
                    } catch (Exception e) {
                        lobby.getLogger().warning(String.format(
                                "Failed to sync chunk [%d, %d]: %s", chunkX, chunkZ, e.getMessage()));
                    }
                }
            }

            final int finalSynced = synced;
            final long finalSize = totalSize;
            Bukkit.getScheduler().runTask(lobby,
                    () -> ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.SYNC_COMMAND.AREA_SYNCED
                            .replace("%chunks%", String.valueOf(finalSynced))
                            .replace("%size%", String.format("%.2f", finalSize / 1024.0))));
        });
    }

    @Command(name = "world")
    public void worldSync(@Sender Player player) {
        ColorUtil.sendMessage(player, "&eStarting world synchronization...");
        World world = player.getWorld();
        WorldBorder border = world.getWorldBorder();

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            try {
                Serializer.WorldSyncData worldData = new Serializer.WorldSyncData();
                worldData.worldName = world.getName();

                Serializer.WorldBorderData borderData = new Serializer.WorldBorderData();
                borderData.centerX = border.getCenter().getX();
                borderData.centerZ = border.getCenter().getZ();
                borderData.size = border.getSize();
                borderData.damageAmount = border.getDamageAmount();
                borderData.damageBuffer = border.getDamageBuffer();
                borderData.warningDistance = border.getWarningDistance();
                borderData.warningTime = border.getWarningTime();
                worldData.border = borderData;

                Map<String, String> gameRules = new HashMap<>();
                String[] rules = world.getGameRules();
                for (String rule : rules) {
                    gameRules.put(rule, world.getGameRuleValue(rule));
                }
                worldData.gameRules = gameRules;

                worldData.difficulty = world.getDifficulty().name();
                worldData.pvp = world.getPVP();
                worldData.time = world.getTime();
                worldData.storm = world.hasStorm();
                worldData.thundering = world.isThundering();

                byte[] serialized = Serializer.serialize(worldData);
                syncService.broadcastEvent(SyncEventType.WORLD_SYNC, serialized);

                Bukkit.getScheduler().runTask(lobby,
                        () -> ColorUtil.sendMessage(player, "&aWorld settings synchronized successfully!"));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(lobby,
                        () -> ColorUtil.sendMessage(player, "&cFailed to sync world: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    @Command(name = "parkour")
    public void parkourSync(@Sender Player player) {
        ColorUtil.sendMessage(player, "&eStarting parkour synchronization...");

        Bukkit.getScheduler().runTask(lobby, () -> {
            parkourService.refreshParkours();
            syncService.broadcastEvent(SyncEventType.PARKOUR_SYNC, new byte[0]);
            ColorUtil.sendMessage(player, "&aParkour courses synchronized successfully!");
        });
    }

    @Command(name = "full")
    public void fullSync(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.FULL_SYNCING);
        syncService.performFullSync();
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.SYNC_COMMAND.FULL_SENT);
    }
}

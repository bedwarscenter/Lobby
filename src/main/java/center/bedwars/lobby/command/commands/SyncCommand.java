package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.CarbonDependency;
import center.bedwars.lobby.sync.LobbySyncManager;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.handlers.ChunkSnapshotSyncHandler;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;
import xyz.refinedev.spigot.features.chunk.snapshot.ICarbonChunkSnapshot;

@Register(name = "sync")
@Requires("bedwarslobby.command.sync")
public class SyncCommand {

    private final Lobby lobby = Lobby.getINSTANCE();
    private final LobbySyncManager syncManager;
    private final IChunkAPI chunkAPI;

    public SyncCommand() {
        this.syncManager = Lobby.getManagerStorage().getManager(LobbySyncManager.class);

        DependencyManager depManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        CarbonDependency carbon = depManager.getCarbon();

        if (!carbon.isApiAvailable()) {
            throw new IllegalStateException("Carbon API is required for sync commands!");
        }

        this.chunkAPI = carbon.getChunkRegistry();
    }

    @Command(name = "")
    public void main(@Sender CommandSender sender) {
        sender.sendMessage("§8§m--------------------");
        sender.sendMessage("§6§lLobby Sync Commands");
        sender.sendMessage("§e/sync config §7- Push config to all lobbies");
        sender.sendMessage("§e/sync chunk §7- Sync current chunk");
        sender.sendMessage("§e/sync area <radius> §7- Sync chunk area");
        sender.sendMessage("§e/sync full §7- Full lobby synchronization");
        sender.sendMessage("§8§m--------------------");
    }

    @Command(name = "config")
    public void configPush(@Sender CommandSender sender) {
        sender.sendMessage("§aPushing configuration to all lobbies...");

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            long reloadTime = ConfigurationManager.reloadConfigurations();

            JsonObject data = new JsonObject();
            data.addProperty("configType", "all");
            data.addProperty("reloadTime", reloadTime);

            syncManager.broadcastEvent(SyncEventType.CONFIG_PUSH, data);

            Bukkit.getScheduler().runTask(lobby, () ->
                    sender.sendMessage(String.format("§aConfiguration pushed! (Reload time: %dms)", reloadTime))
            );
        });
    }

    @Command(name = "chunk")
    public void chunkSync(@Sender Player player) {
        player.sendMessage("§aSynchronizing current chunk...");

        Chunk chunk = player.getLocation().getChunk();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            try {
                ICarbonChunkSnapshot<?> snapshot = chunkAPI.takeSnapshot(chunk);
                byte[] snapshotData = ChunkSnapshotSyncHandler.serializeSnapshot(snapshot);

                JsonObject data = SyncDataSerializer.serializeChunkSnapshot(chunkX, chunkZ, snapshotData);
                syncManager.broadcastEvent(SyncEventType.CHUNK_SNAPSHOT, data);

                Bukkit.getScheduler().runTask(lobby, () ->
                        player.sendMessage(String.format(
                                "§aChunk [%d, %d] synchronized! (Size: %.2f KB)",
                                chunkX, chunkZ, snapshotData.length / 1024.0
                        ))
                );
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(lobby, () ->
                        player.sendMessage("§cFailed to synchronize chunk: " + e.getMessage())
                );
                e.printStackTrace();
            }
        });
    }

    @Command(name = "area")
    public void areaSync(@Sender Player player, @Named("radius") @Default("5") @Range(min = 1, max = 20) int radius) {
        if (radius < 1 || radius > 20) {
            player.sendMessage("§cRadius must be between 1 and 20!");
            return;
        }

        player.sendMessage(String.format("§aSynchronizing area with radius %d chunks...", radius));

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
                        ICarbonChunkSnapshot<?> snapshot = chunkAPI.takeSnapshot(chunk);
                        byte[] snapshotData = ChunkSnapshotSyncHandler.serializeSnapshot(snapshot);

                        JsonObject data = SyncDataSerializer.serializeChunkSnapshot(chunkX, chunkZ, snapshotData);
                        syncManager.broadcastEvent(SyncEventType.CHUNK_SNAPSHOT, data);

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
                    player.sendMessage(String.format(
                            "§aArea synchronized! %d chunks (Total: %.2f KB)",
                            finalSynced, finalSize / 1024.0
                    ))
            );
        });
    }

    @Command(name = "full")
    public void fullSync(@Sender CommandSender sender) {
        sender.sendMessage("§aInitiating full lobby synchronization...");
        syncManager.performFullSync();
        sender.sendMessage("§aFull synchronization request sent!");
    }
}
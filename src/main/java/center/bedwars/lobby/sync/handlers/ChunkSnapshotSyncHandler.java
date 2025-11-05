package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.CarbonDependency;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;
import xyz.refinedev.spigot.features.chunk.snapshot.ICarbonChunkSnapshot;

import java.io.*;

public class ChunkSnapshotSyncHandler implements ISyncHandler {

    private final IChunkAPI chunkAPI;

    public ChunkSnapshotSyncHandler() {
        DependencyManager depManager = Lobby
                .getManagerStorage()
                .getManager(DependencyManager.class);

        CarbonDependency carbon = depManager.getCarbon();

        if (!carbon.isApiAvailable()) {
            throw new IllegalStateException("Carbon API is not available!");
        }

        this.chunkAPI = carbon.getChunkRegistry();
    }

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();

        int chunkX = data.get("chunkX").getAsInt();
        int chunkZ = data.get("chunkZ").getAsInt();

        if (data.get("data").getAsString().length() > 50000) {
            Lobby.getINSTANCE().getLogger().warning("Chunk snapshot too large, skipping");
            return;
        }

        byte[] snapshotData = SyncDataSerializer.deserializeChunkSnapshotData(data);

        World world = Bukkit.getWorld("world");
        if (world == null) {
            return;
        }

        chunkAPI.getChunkAtAsync(world, chunkX, chunkZ, true, true, chunk -> {
            try {
                ICarbonChunkSnapshot<?> snapshot = deserializeSnapshot(snapshotData);
                chunkAPI.restoreSnapshot(chunk, snapshot);
                chunkAPI.refreshChunk(chunk);
            } catch (Exception e) {
                Lobby.getINSTANCE().getLogger().severe("Failed to restore chunk snapshot");
            }
        });
    }

    private ICarbonChunkSnapshot<?> deserializeSnapshot(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (ICarbonChunkSnapshot<?>) ois.readObject();
        }
    }

    public static byte[] serializeSnapshot(ICarbonChunkSnapshot<?> snapshot) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(snapshot);
            oos.flush();
            return bos.toByteArray();
        }
    }
}
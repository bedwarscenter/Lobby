package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;
import xyz.refinedev.spigot.features.chunk.snapshot.ICarbonChunkSnapshot;

import java.io.*;

public class ChunkSnapshotSyncHandler implements ISyncHandler {

    private final IChunkAPI chunkAPI;

    public ChunkSnapshotSyncHandler() {
        this.chunkAPI = Lobby.getManagerStorage()
                .getManager(DependencyManager.class)
                .getCarbon()
                .getChunkRegistry();
    }

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        int cx = data.get("cx").getAsInt();
        int cz = data.get("cz").getAsInt();
        byte[] snapshotData = SyncDataSerializer.deserializeChunkSnapshotData(data);

        chunkAPI.getChunkAtAsync(Bukkit.getWorld("world"), cx, cz, true, true, chunk -> {
            try {
                ICarbonChunkSnapshot<?> snapshot = deserialize(snapshotData);
                chunkAPI.restoreSnapshot(chunk, snapshot);
                chunkAPI.refreshChunk(chunk);
            } catch (Exception ignored) {}
        });
    }

    private ICarbonChunkSnapshot<?> deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (ICarbonChunkSnapshot<?>) ois.readObject();
        }
    }

    public static byte[] serialize(ICarbonChunkSnapshot<?> snapshot) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(snapshot);
            oos.flush();
            return bos.toByteArray();
        }
    }
}
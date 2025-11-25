package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.KryoSerializer;
import center.bedwars.lobby.sync.serialization.KryoSerializer.ChunkData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
        try {
            ChunkData chunkData = KryoSerializer.deserialize(event.getData(), ChunkData.class);

            chunkAPI.getChunkAtAsync(Bukkit.getWorld("world"), chunkData.chunkX, chunkData.chunkZ, true, true, chunk -> Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                try {
                    restoreChunkFromData(chunk, chunkData.snapshotData);
                    chunkAPI.refreshChunk(chunk);
                } catch (Exception e) {
                    Lobby.getINSTANCE().getLogger().warning("Failed to restore chunk: " + e.getMessage());
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreChunkFromData(Chunk chunk, byte[] data) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bis);
             DataInputStream dis = new DataInputStream(gis)) {

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        int typeId = dis.readInt();
                        byte blockData = dis.readByte();

                        if (typeId != 0) {
                            Material material = Material.getMaterial(typeId);
                            if (material != null) {
                                chunk.getBlock(x, y, z).setTypeIdAndData(typeId, blockData, false);
                            }
                        }
                    }
                }
            }
        }
    }

    public static byte[] serialize(Chunk chunk) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(bos);
             DataOutputStream dos = new DataOutputStream(gos)) {

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        org.bukkit.block.Block block = chunk.getBlock(x, y, z);
                        dos.writeInt(block.getTypeId());
                        dos.writeByte(block.getData());
                    }
                }
            }

            dos.flush();
            gos.finish();
        }
        return bos.toByteArray();
    }
}
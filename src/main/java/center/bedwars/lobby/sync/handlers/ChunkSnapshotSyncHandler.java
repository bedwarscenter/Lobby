package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.Serializer;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ChunkSnapshotSyncHandler implements ISyncHandler {

    private static final int COMPRESSION_LEVEL = Deflater.BEST_SPEED;

    private final Lobby plugin;

    @com.google.inject.Inject
    public ChunkSnapshotSyncHandler(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(SyncEvent event) {
        try {
            Serializer.ChunkData chunkData = Serializer.deserialize(event.getData(), Serializer.ChunkData.class);

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    org.bukkit.Chunk bukkitChunk = Bukkit.getWorld("world").getChunkAt(chunkData.chunkX,
                            chunkData.chunkZ);
                    if (!bukkitChunk.isLoaded()) {
                        bukkitChunk.load(true);
                    }
                    restoreChunk(((CraftChunk) bukkitChunk).getHandle(), chunkData.snapshotData);
                    bukkitChunk.getWorld().refreshChunk(chunkData.chunkX, chunkData.chunkZ);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to restore chunk: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] serialize(org.bukkit.Chunk bukkitChunk) throws IOException {
        Chunk nmsChunk = ((CraftChunk) bukkitChunk).getHandle();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos)) {

            ChunkSection[] sections = nmsChunk.getSections();
            dos.writeByte(sections.length);

            for (int i = 0; i < sections.length; i++) {
                ChunkSection section = sections[i];

                if (section == null || section.a()) {
                    dos.writeBoolean(false);
                    continue;
                }

                dos.writeBoolean(true);
                dos.writeByte(i);

                byte[] sectionData = serializeSection(section);
                byte[] compressed = compress(sectionData);

                dos.writeInt(compressed.length);
                dos.write(compressed);
            }

            return baos.toByteArray();
        }
    }

    private static byte[] serializeSection(ChunkSection section) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos)) {

            char[] blockIds = section.getIdArray();
            dos.writeInt(blockIds.length);

            for (char id : blockIds) {
                dos.writeChar(id);
            }

            byte[] skyLight = section.getSkyLightArray().a();
            byte[] blockLight = section.getEmittedLightArray().a();

            dos.writeInt(skyLight.length);
            dos.write(skyLight);

            dos.writeInt(blockLight.length);
            dos.write(blockLight);

            return baos.toByteArray();
        }
    }

    private static void restoreChunk(Chunk nmsChunk, byte[] data) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {

            int sectionCount = dis.readByte() & 0xFF;
            ChunkSection[] sections = new ChunkSection[sectionCount];

            for (int i = 0; i < sectionCount; i++) {
                boolean hasData = dis.readBoolean();

                if (!hasData) {
                    sections[i] = null;
                    continue;
                }

                int sectionIndex = dis.readByte() & 0xFF;
                int compressedLength = dis.readInt();

                byte[] compressed = new byte[compressedLength];
                dis.readFully(compressed);

                byte[] decompressed = decompress(compressed);
                sections[sectionIndex] = deserializeSection(decompressed, sectionIndex);
            }

            setSections(nmsChunk, sections);
        }
    }

    private static ChunkSection deserializeSection(byte[] data, int yPos) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {

            ChunkSection section = new ChunkSection(yPos << 4, true);

            int idArrayLength = dis.readInt();
            char[] blockIds = new char[idArrayLength];

            for (int i = 0; i < idArrayLength; i++) {
                blockIds[i] = dis.readChar();
            }

            setIdArray(section, blockIds);

            int skyLightLength = dis.readInt();
            byte[] skyLight = new byte[skyLightLength];
            dis.readFully(skyLight);
            setLightArray(section.getSkyLightArray(), skyLight);

            int blockLightLength = dis.readInt();
            byte[] blockLight = new byte[blockLightLength];
            dis.readFully(blockLight);
            setLightArray(section.getEmittedLightArray(), blockLight);

            section.recalcBlockCounts();

            return section;
        }
    }

    private static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater(COMPRESSION_LEVEL);
        deflater.setInput(data);
        deflater.finish();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length)) {
            byte[] buffer = new byte[8192];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                baos.write(buffer, 0, count);
            }

            deflater.end();
            return baos.toByteArray();
        }
    }

    private static byte[] decompress(byte[] data) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length * 2)) {
            byte[] buffer = new byte[8192];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }

            inflater.end();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to decompress data", e);
        }
    }

    private static void setIdArray(ChunkSection section, char[] blockIds) {
        try {
            java.lang.reflect.Field field = ChunkSection.class.getDeclaredField("blockIds");
            field.setAccessible(true);
            field.set(section, blockIds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set block IDs", e);
        }
    }

    private static void setSections(Chunk chunk, ChunkSection[] sections) {
        try {
            java.lang.reflect.Field field = Chunk.class.getDeclaredField("sections");
            field.setAccessible(true);
            field.set(chunk, sections);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set sections", e);
        }
    }

    private static void setLightArray(net.minecraft.server.v1_8_R3.NibbleArray nibbleArray, byte[] data) {
        try {
            java.lang.reflect.Field field = net.minecraft.server.v1_8_R3.NibbleArray.class.getDeclaredField("a");
            field.setAccessible(true);
            field.set(nibbleArray, data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set light array", e);
        }
    }
}

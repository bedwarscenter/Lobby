package center.bedwars.lobby.sync.serialization;

import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import org.bukkit.Location;
import org.bukkit.Material;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class Serializer {

    private static final byte WORLD_MAIN = 0;
    private static final byte WORLD_NETHER = 1;
    private static final byte WORLD_END = 2;

    private static final Map<String, Short> MATERIAL_CACHE = new HashMap<>();
    private static final Map<Short, Material> MATERIAL_REVERSE = new HashMap<>();

    static {
        short id = 0;
        for (Material mat : Material.values()) {
            MATERIAL_CACHE.put(mat.name(), id);
            MATERIAL_REVERSE.put(id, mat);
            id++;
        }
    }

    public static byte[] serialize(Object obj) {
        if (obj instanceof SyncEvent) return serializeSyncEvent((SyncEvent) obj);
        if (obj instanceof BlockData) return serializeBlock((BlockData) obj);
        if (obj instanceof NPCData) return serializeNPC((NPCData) obj);
        if (obj instanceof HologramData) return serializeHologram((HologramData) obj);
        if (obj instanceof ChunkData) return serializeChunk((ChunkData) obj);
        if (obj instanceof WorldSyncData) return serializeWorld((WorldSyncData) obj);
        throw new IllegalArgumentException("Unknown type: " + obj.getClass());
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        if (clazz == SyncEvent.class) return clazz.cast(deserializeSyncEvent(data));
        if (clazz == BlockData.class) return clazz.cast(deserializeBlock(data));
        if (clazz == NPCData.class) return clazz.cast(deserializeNPC(data));
        if (clazz == HologramData.class) return clazz.cast(deserializeHologram(data));
        if (clazz == ChunkData.class) return clazz.cast(deserializeChunk(data));
        if (clazz == WorldSyncData.class) return clazz.cast(deserializeWorld(data));
        throw new IllegalArgumentException("Unknown type: " + clazz);
    }

    private static byte[] serializeSyncEvent(SyncEvent event) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeUTF(event.getLobbyId());
            dos.writeByte(event.getType().ordinal());
            dos.writeInt(event.getData().length);
            dos.write(event.getData());
            dos.writeLong(event.getTimestamp());

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize SyncEvent", e);
        }
    }

    private static SyncEvent deserializeSyncEvent(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            String lobbyId = dis.readUTF();
            SyncEventType type = SyncEventType.values()[dis.readByte()];
            int dataLength = dis.readInt();
            byte[] eventData = new byte[dataLength];
            dis.readFully(eventData);
            long timestamp = dis.readLong();

            SyncEvent event = new SyncEvent(lobbyId, type, eventData);
            return event;
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize SyncEvent", e);
        }
    }

    private static byte[] serializeLocation(LocationData loc) {
        ByteBuffer buffer = ByteBuffer.allocate(26);
        buffer.put(loc.worldId);
        buffer.putInt((int) (loc.x * 1000));
        buffer.putInt((int) (loc.y * 1000));
        buffer.putInt((int) (loc.z * 1000));
        buffer.putShort((short) (loc.yaw * 100));
        buffer.putShort((short) (loc.pitch * 100));
        return buffer.array();
    }

    private static LocationData deserializeLocation(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte worldId = buffer.get();
        double x = buffer.getInt() / 1000.0;
        double y = buffer.getInt() / 1000.0;
        double z = buffer.getInt() / 1000.0;
        float yaw = buffer.getShort() / 100f;
        float pitch = buffer.getShort() / 100f;
        return new LocationData(worldId, x, y, z, yaw, pitch);
    }

    private static byte[] serializeBlock(BlockData block) {
        ByteBuffer buffer = ByteBuffer.allocate(28);
        buffer.put(serializeLocation(block.location));
        buffer.putShort(MATERIAL_CACHE.get(block.material));
        buffer.put(block.data);
        return buffer.array();
    }

    private static BlockData deserializeBlock(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] locData = new byte[26];
        buffer.get(locData);
        LocationData loc = deserializeLocation(locData);
        Material mat = MATERIAL_REVERSE.get(buffer.getShort());
        byte blockData = buffer.get();
        BlockData result = new BlockData();
        result.location = loc;
        result.material = mat.name();
        result.data = blockData;
        return result;
    }

    private static byte[] serializeNPC(NPCData npc) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeShort(npc.npcId);
            dos.writeUTF(npc.name);
            dos.write(serializeLocation(npc.location));
            dos.writeUTF(npc.texture);
            dos.writeUTF(npc.signature);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static NPCData deserializeNPC(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            NPCData npc = new NPCData();
            npc.npcId = dis.readShort();
            npc.name = dis.readUTF();
            byte[] locData = new byte[26];
            dis.readFully(locData);
            npc.location = deserializeLocation(locData);
            npc.texture = dis.readUTF();
            npc.signature = dis.readUTF();
            return npc;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] serializeHologram(HologramData holo) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeUTF(holo.hologramId);
            dos.write(serializeLocation(holo.location));
            dos.writeByte(holo.lines.length);
            for (String line : holo.lines) {
                dos.writeUTF(line);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HologramData deserializeHologram(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            HologramData holo = new HologramData();
            holo.hologramId = dis.readUTF();
            byte[] locData = new byte[26];
            dis.readFully(locData);
            holo.location = deserializeLocation(locData);
            int lineCount = dis.readByte() & 0xFF;
            holo.lines = new String[lineCount];
            for (int i = 0; i < lineCount; i++) {
                holo.lines[i] = dis.readUTF();
            }
            return holo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] serializeChunk(ChunkData chunk) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeShort(chunk.chunkX);
            dos.writeShort(chunk.chunkZ);
            dos.writeInt(chunk.snapshotData.length);
            dos.write(chunk.snapshotData);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ChunkData deserializeChunk(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            ChunkData chunk = new ChunkData();
            chunk.chunkX = dis.readShort();
            chunk.chunkZ = dis.readShort();
            int length = dis.readInt();
            chunk.snapshotData = new byte[length];
            dis.readFully(chunk.snapshotData);
            return chunk;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] serializeWorld(WorldSyncData world) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeUTF(world.worldName);
            dos.writeInt((int) (world.border.centerX * 1000));
            dos.writeInt((int) (world.border.centerZ * 1000));
            dos.writeInt((int) world.border.size);
            dos.writeByte(getDifficultyId(world.difficulty));
            dos.writeBoolean(world.pvp);
            dos.writeLong(world.time);
            dos.writeBoolean(world.storm);
            dos.writeBoolean(world.thundering);
            dos.writeByte(world.gameRules.size());
            for (Map.Entry<String, String> entry : world.gameRules.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static WorldSyncData deserializeWorld(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            WorldSyncData world = new WorldSyncData();
            world.worldName = dis.readUTF();
            world.border = new WorldBorderData();
            world.border.centerX = dis.readInt() / 1000.0;
            world.border.centerZ = dis.readInt() / 1000.0;
            world.border.size = dis.readInt();
            world.difficulty = getDifficultyName(dis.readByte());
            world.pvp = dis.readBoolean();
            world.time = dis.readLong();
            world.storm = dis.readBoolean();
            world.thundering = dis.readBoolean();
            int ruleCount = dis.readByte() & 0xFF;
            world.gameRules = new HashMap<>();
            for (int i = 0; i < ruleCount; i++) {
                world.gameRules.put(dis.readUTF(), dis.readUTF());
            }
            return world;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte getDifficultyId(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> 1;
            case "NORMAL" -> 2;
            case "HARD" -> 3;
            default -> 0;
        };
    }

    private static String getDifficultyName(byte id) {
        return switch (id) {
            case 1 -> "EASY";
            case 2 -> "NORMAL";
            case 3 -> "HARD";
            default -> "PEACEFUL";
        };
    }

    public static class LocationData {
        public byte worldId;
        public double x, y, z;
        public float yaw, pitch;

        public LocationData() {}

        public LocationData(byte worldId, double x, double y, double z, float yaw, float pitch) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public LocationData(Location loc) {
            if (loc != null && loc.getWorld() != null) {
                this.worldId = getWorldId(loc.getWorld().getName());
                this.x = loc.getX();
                this.y = loc.getY();
                this.z = loc.getZ();
                this.yaw = loc.getYaw();
                this.pitch = loc.getPitch();
            }
        }

        public Location toLocation(org.bukkit.Server server) {
            String worldName = getWorldName(worldId);
            if (worldName == null) return null;
            return new Location(server.getWorld(worldName), x, y, z, yaw, pitch);
        }

        private static byte getWorldId(String worldName) {
            if ("world".equals(worldName)) return WORLD_MAIN;
            if ("world_nether".equals(worldName)) return WORLD_NETHER;
            if ("world_the_end".equals(worldName)) return WORLD_END;
            return (byte) worldName.hashCode();
        }

        private static String getWorldName(byte worldId) {
            return switch (worldId) {
                case WORLD_NETHER -> "world_nether";
                case WORLD_END -> "world_the_end";
                default -> "world";
            };
        }
    }

    public static class BlockData {
        public LocationData location;
        public String material;
        public byte data;

        public BlockData() {}

        public BlockData(Location loc, Material mat, byte data) {
            this.location = new LocationData(loc);
            this.material = mat.name();
            this.data = data;
        }

        public Material getMaterial() {
            return Material.getMaterial(material);
        }
    }

    public static class NPCData {
        public short npcId;
        public String name;
        public LocationData location;
        public String texture;
        public String signature;

        public NPCData() {}

        public NPCData(String npcId, String name, Location loc, String texture, String signature) {
            try {
                this.npcId = Short.parseShort(npcId);
            } catch (NumberFormatException e) {
                this.npcId = (short) npcId.hashCode();
            }
            this.name = name;
            this.location = new LocationData(loc);
            this.texture = texture != null ? texture : "";
            this.signature = signature != null ? signature : "";
        }

        public String getNpcIdAsString() {
            return String.valueOf(npcId);
        }
    }

    public static class HologramData {
        public String hologramId;
        public LocationData location;
        public String[] lines;

        public HologramData() {}

        public HologramData(String hologramId, Location loc, String[] lines) {
            this.hologramId = hologramId;
            this.location = new LocationData(loc);
            this.lines = lines;
        }
    }

    public static class WorldSyncData {
        public String worldName;
        public byte worldId;
        public WorldBorderData border;
        public Map<String, String> gameRules;
        public String difficulty;
        public boolean pvp;
        public long time;
        public boolean storm;
        public boolean thundering;

        public WorldSyncData() {}
    }

    public static class WorldBorderData {
        public double centerX, centerZ;
        public double size;
        public double damageAmount;
        public double damageBuffer;
        public int warningDistance;
        public int warningTime;

        public WorldBorderData() {}
    }

    public static class ChunkData {
        public int chunkX, chunkZ;
        public byte[] snapshotData;

        public ChunkData() {}

        public ChunkData(int x, int z, byte[] data) {
            this.chunkX = x;
            this.chunkZ = z;
            this.snapshotData = data;
        }
    }
}
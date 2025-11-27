package center.bedwars.lobby.sync.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@SuppressWarnings("unused")
public final class KryoSerializer {

    private static final Pool<Kryo> kryoPool = new Pool<>(true, false, 32) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setReferences(false);

            kryo.register(LocationData.class);
            kryo.register(BlockData.class);
            kryo.register(NPCData.class);
            kryo.register(HologramData.class);
            kryo.register(WorldSyncData.class);
            kryo.register(WorldBorderData.class);
            kryo.register(ChunkData.class);

            return kryo;
        }
    };

    private static final Pool<Output> outputPool = new Pool<>(true, false, 32) {
        protected Output create() {
            return new Output(1024, -1);
        }
    };

    private static final Pool<Input> inputPool = new Pool<>(true, false, 32) {
        protected Input create() {
            return new Input();
        }
    };

    public static byte[] serialize(Object obj) {
        Kryo kryo = kryoPool.obtain();
        Output output = outputPool.obtain();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output.setOutputStream(baos);
            kryo.writeClassAndObject(output, obj);
            output.flush();
            return baos.toByteArray();
        } finally {
            kryoPool.free(kryo);
            outputPool.free(output);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();

        try {
            input.setBuffer(data);
            return clazz.cast(kryo.readClassAndObject(input));
        } finally {
            kryoPool.free(kryo);
            inputPool.free(input);
        }
    }

    public static Object deserialize(byte[] data) {
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();

        try {
            input.setBuffer(data);
            return kryo.readClassAndObject(input);
        } finally {
            kryoPool.free(kryo);
            inputPool.free(input);
        }
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
            if ("world".equals(worldName)) return 0;
            if ("world_nether".equals(worldName)) return 1;
            if ("world_the_end".equals(worldName)) return 2;
            return (byte) worldName.hashCode();
        }

        private static String getWorldName(byte worldId) {
            return switch (worldId) {
                case 1 -> "world_nether";
                case 2 -> "world_the_end";
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
        public String difficulty; // Changed from byte to String
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
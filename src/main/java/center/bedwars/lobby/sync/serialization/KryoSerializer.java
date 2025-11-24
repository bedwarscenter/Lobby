package center.bedwars.lobby.sync.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public final class KryoSerializer {

    private static final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 32) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setReferences(false);

            kryo.register(PlayerSyncData.class);
            kryo.register(EntitySyncData.class);
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

    private static final Pool<Output> outputPool = new Pool<Output>(true, false, 32) {
        protected Output create() {
            return new Output(1024, -1);
        }
    };

    private static final Pool<Input> inputPool = new Pool<Input>(true, false, 32) {
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
        public String world;
        public double x, y, z;
        public float yaw, pitch;

        public LocationData() {}

        public LocationData(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public LocationData(Location loc) {
            if (loc != null && loc.getWorld() != null) {
                this.world = loc.getWorld().getName();
                this.x = loc.getX();
                this.y = loc.getY();
                this.z = loc.getZ();
                this.yaw = loc.getYaw();
                this.pitch = loc.getPitch();
            }
        }

        public Location toLocation(org.bukkit.Server server) {
            if (world == null) return null;
            return new Location(server.getWorld(world), x, y, z, yaw, pitch);
        }
    }

    public static class PlayerSyncData {
        public String lobbyId;
        public String action;
        public String uuid;
        public String name;
        public String texture;
        public String signature;

        public PlayerSyncData() {}

        public PlayerSyncData(String lobbyId, String action, String uuid, String name, String texture, String signature) {
            this.lobbyId = lobbyId;
            this.action = action;
            this.uuid = uuid;
            this.name = name;
            this.texture = texture;
            this.signature = signature;
        }
    }

    public static class EntitySyncData {
        public String lobbyId;
        public String uuid;
        public String name;
        public String texture;
        public String signature;
        public LocationData location;
        public boolean sneaking;
        public boolean sprinting;
        public int heldSlot;
        public byte swingType;

        public EntitySyncData() {}

        public EntitySyncData(String lobbyId, String uuid, String name, String texture, String signature,
                              LocationData location, boolean sneaking, boolean sprinting, int heldSlot, byte swingType) {
            this.lobbyId = lobbyId;
            this.uuid = uuid;
            this.name = name;
            this.texture = texture;
            this.signature = signature;
            this.location = location;
            this.sneaking = sneaking;
            this.sprinting = sprinting;
            this.heldSlot = heldSlot;
            this.swingType = swingType;
        }

        public static EntitySyncData fromLocation(String lobbyId, String uuid, String name,
                                                  String texture, String signature, Location loc,
                                                  boolean sneaking, boolean sprinting, int heldSlot, byte swingType) {
            LocationData locData = new LocationData(
                    loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(),
                    loc.getYaw(), loc.getPitch()
            );
            return new EntitySyncData(lobbyId, uuid, name, texture, signature, locData, sneaking, sprinting, heldSlot, swingType);
        }

        public Location toLocation(org.bukkit.Server server) {
            if (location == null) return null;
            return new Location(
                    server.getWorld(location.world),
                    location.x, location.y, location.z,
                    location.yaw, location.pitch
            );
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
    }

    public static class NPCData {
        public String npcId;
        public String name;
        public LocationData location;
        public String texture;
        public String signature;

        public NPCData() {}

        public NPCData(String npcId, String name, Location loc, String texture, String signature) {
            this.npcId = npcId;
            this.name = name;
            this.location = new LocationData(loc);
            this.texture = texture != null ? texture : "";
            this.signature = signature != null ? signature : "";
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
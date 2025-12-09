package center.bedwars.lobby.sync.serialization;

import org.bukkit.Location;
import org.bukkit.Server;

public class LocationData {

    private static final byte WORLD_MAIN = 0;
    private static final byte WORLD_NETHER = 1;
    private static final byte WORLD_END = 2;

    public byte worldId;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public LocationData() {
    }

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

    public Location toLocation(Server server) {
        String worldName = getWorldName(worldId);
        return new Location(server.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public static byte getWorldId(String worldName) {
        if ("world".equals(worldName))
            return WORLD_MAIN;
        if ("world_nether".equals(worldName))
            return WORLD_NETHER;
        if ("world_the_end".equals(worldName))
            return WORLD_END;
        return (byte) worldName.hashCode();
    }

    public static String getWorldName(byte worldId) {
        return switch (worldId) {
            case WORLD_NETHER -> "world_nether";
            case WORLD_END -> "world_the_end";
            default -> "world";
        };
    }
}

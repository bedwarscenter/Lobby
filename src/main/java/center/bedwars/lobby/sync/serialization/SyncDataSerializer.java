package center.bedwars.lobby.sync.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;

public class SyncDataSerializer {

    public static ByteBuf serializeLocation(Location loc) {
        ByteBuf buf = Unpooled.buffer();
        if (loc == null || loc.getWorld() == null) {
            buf.writeBoolean(false);
            return buf;
        }
        buf.writeBoolean(true);
        writeUTF(buf, loc.getWorld().getName());
        buf.writeDouble(loc.getX());
        buf.writeDouble(loc.getY());
        buf.writeDouble(loc.getZ());
        buf.writeFloat(loc.getYaw());
        buf.writeFloat(loc.getPitch());
        return buf;
    }

    public static Location deserializeLocation(ByteBuf buf, Server server) {
        if (!buf.readBoolean()) return null;
        String world = readUTF(buf);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();
        return new Location(server.getWorld(world), x, y, z, yaw, pitch);
    }

    public static ByteBuf serializeBlockData(Location loc, Material material, byte data) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(serializeLocation(loc));
        writeUTF(buf, material.name());
        buf.writeByte(data);
        return buf;
    }

    public static ByteBuf serializeChunkSnapshot(int chunkX, int chunkZ, byte[] snapshotData) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeInt(snapshotData.length);
        buf.writeBytes(snapshotData);
        return buf;
    }

    public static byte[] deserializeChunkSnapshotData(ByteBuf buf) {
        int len = buf.readInt();
        byte[] data = new byte[len];
        buf.readBytes(data);
        return data;
    }

    public static ByteBuf serializeNPCData(String npcId, String name, Location loc, String texture, String signature) {
        ByteBuf buf = Unpooled.buffer();
        writeUTF(buf, npcId);
        writeUTF(buf, name);
        buf.writeBytes(serializeLocation(loc));
        writeUTF(buf, texture == null ? "" : texture);
        writeUTF(buf, signature == null ? "" : signature);
        return buf;
    }

    public static ByteBuf serializeHologramData(String hologramId, Location loc, String[] lines) {
        ByteBuf buf = Unpooled.buffer();
        writeUTF(buf, hologramId);
        buf.writeBytes(serializeLocation(loc));
        buf.writeInt(lines.length);
        for (String line : lines) writeUTF(buf, line);
        return buf;
    }

    public static String[] deserializeHologramLines(ByteBuf buf) {
        int len = buf.readInt();
        String[] lines = new String[len];
        for (int i = 0; i < len; i++) lines[i] = readUTF(buf);
        return lines;
    }

    public static void writeUTF(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readUTF(ByteBuf buf) {
        int len = buf.readShort();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
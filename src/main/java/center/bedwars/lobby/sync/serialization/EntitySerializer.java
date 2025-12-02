package center.bedwars.lobby.sync.serialization;

import center.bedwars.lobby.sync.packet.EntitySyncPacketType;
import java.io.*;
import java.util.*;

public final class EntitySerializer {

    public static byte[] serializeBatch(List<EntityPacket> packets) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeShort(packets.size());
        for (EntityPacket packet : packets) {
            packet.write(out);
        }

        return baos.toByteArray();
    }

    public static List<EntityPacket> deserializeBatch(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);

        int size = in.readShort();
        List<EntityPacket> packets = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            byte typeId = in.readByte();
            EntitySyncPacketType type = EntitySyncPacketType.fromId(typeId);

            EntityPacket packet = createPacket(type);
            packet.read(in);
            packets.add(packet);
        }

        return packets;
    }

    private static EntityPacket createPacket(EntitySyncPacketType type) {
        return switch (type) {
            case SPAWN -> new SpawnEntityPacket();
            case MOVE -> new MoveEntityPacket();
            case TELEPORT -> new TeleportEntityPacket();
            case SNEAK -> new SneakEntityPacket();
            case SPRINT -> new SprintEntityPacket();
            case SWING -> new SwingEntityPacket();
            case SLOT -> new SlotEntityPacket();
            case DESPAWN -> new DespawnEntityPacket();
        };
    }

    public interface EntityPacket {
        EntitySyncPacketType getType();
        void write(DataOutputStream out) throws IOException;
        void read(DataInputStream in) throws IOException;
        UUID getPlayerId();
        byte getLobbyId();
    }

    public static class SpawnEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public String name;
        public String texture;
        public String signature;
        public double x, y, z;
        public float yaw, pitch;
        public boolean sneak, sprint;
        public byte slot;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.SPAWN; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeUTF(name);
            out.writeUTF(texture);
            out.writeUTF(signature);
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            out.writeByte((sneak ? 1 : 0) | (sprint ? 2 : 0));
            out.writeByte(slot);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            name = in.readUTF();
            texture = in.readUTF();
            signature = in.readUTF();
            x = in.readDouble();
            y = in.readDouble();
            z = in.readDouble();
            yaw = in.readFloat();
            pitch = in.readFloat();
            byte flags = in.readByte();
            sneak = (flags & 1) != 0;
            sprint = (flags & 2) != 0;
            slot = in.readByte();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class MoveEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public byte dx, dy, dz;
        public byte yaw, pitch;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.MOVE; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeByte(dx);
            out.writeByte(dy);
            out.writeByte(dz);
            out.writeByte(yaw);
            out.writeByte(pitch);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            dx = in.readByte();
            dy = in.readByte();
            dz = in.readByte();
            yaw = in.readByte();
            pitch = in.readByte();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class TeleportEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public double x, y, z;
        public float yaw, pitch;
        public byte worldId;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.TELEPORT; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeFloat(yaw);
            out.writeFloat(pitch);
            out.writeByte(worldId);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            x = in.readDouble();
            y = in.readDouble();
            z = in.readDouble();
            yaw = in.readFloat();
            pitch = in.readFloat();
            worldId = in.readByte();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class SneakEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public boolean sneaking;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.SNEAK; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeBoolean(sneaking);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            sneaking = in.readBoolean();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class SprintEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public boolean sprinting;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.SPRINT; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeBoolean(sprinting);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            sprinting = in.readBoolean();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class SwingEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public boolean mainHand;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.SWING; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeBoolean(mainHand);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            mainHand = in.readBoolean();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class SlotEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;
        public byte slot;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.SLOT; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeByte(slot);
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
            slot = in.readByte();
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }

    public static class DespawnEntityPacket implements EntityPacket {
        public byte lobbyId;
        public UUID playerId;

        @Override
        public EntitySyncPacketType getType() { return EntitySyncPacketType.DESPAWN; }

        @Override
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(getType().getId());
            out.writeByte(lobbyId);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
        }

        @Override
        public void read(DataInputStream in) throws IOException {
            lobbyId = in.readByte();
            playerId = new UUID(in.readLong(), in.readLong());
        }

        @Override
        public UUID getPlayerId() { return playerId; }

        @Override
        public byte getLobbyId() { return lobbyId; }
    }
}
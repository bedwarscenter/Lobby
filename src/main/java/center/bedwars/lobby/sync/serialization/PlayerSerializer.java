package center.bedwars.lobby.sync.serialization;

import java.io.*;
import java.util.UUID;

public final class PlayerSerializer {

    public static byte[] serialize(PlayerSyncPacket packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeByte(packet.lobbyId);
        out.writeByte(packet.action.getId());
        out.writeLong(packet.playerId.getMostSignificantBits());
        out.writeLong(packet.playerId.getLeastSignificantBits());
        out.writeUTF(packet.name);
        out.writeUTF(packet.texture);
        out.writeUTF(packet.signature);

        return baos.toByteArray();
    }

    public static PlayerSyncPacket deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);

        byte lobbyId = in.readByte();
        PlayerSyncAction action = PlayerSyncAction.fromId(in.readByte());
        UUID playerId = new UUID(in.readLong(), in.readLong());
        String name = in.readUTF();
        String texture = in.readUTF();
        String signature = in.readUTF();

        return new PlayerSyncPacket(lobbyId, action, playerId, name, texture, signature);
    }

    public enum PlayerSyncAction {
        JOIN((byte) 0),
        QUIT((byte) 1),
        HEARTBEAT((byte) 2);

        private final byte id;

        PlayerSyncAction(byte id) {
            this.id = id;
        }

        public byte getId() {
            return id;
        }

        public static PlayerSyncAction fromId(byte id) {
            for (PlayerSyncAction action : values()) {
                if (action.id == id) return action;
            }
            throw new IllegalArgumentException("Unknown action: " + id);
        }
    }

    public static class PlayerSyncPacket {
        public byte lobbyId;
        public PlayerSyncAction action;
        public UUID playerId;
        public String name;
        public String texture;
        public String signature;

        public PlayerSyncPacket(byte lobbyId, PlayerSyncAction action, UUID playerId,
                                String name, String texture, String signature) {
            this.lobbyId = lobbyId;
            this.action = action;
            this.playerId = playerId;
            this.name = name;
            this.texture = texture;
            this.signature = signature;
        }
    }
}
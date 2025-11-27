package center.bedwars.lobby.sync.packet;

public enum EntitySyncPacketType {
    SPAWN((byte) 0),
    MOVE((byte) 1),
    TELEPORT((byte) 2),
    SNEAK((byte) 3),
    SPRINT((byte) 4),
    SWING((byte) 5),
    SLOT((byte) 6),
    DESPAWN((byte) 7);

    private final byte id;

    EntitySyncPacketType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static EntitySyncPacketType fromId(byte id) {
        for (EntitySyncPacketType type : values()) {
            if (type.id == id) return type;
        }
        throw new IllegalArgumentException("Unknown packet type: " + id);
    }
}
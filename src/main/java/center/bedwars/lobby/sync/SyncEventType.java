package center.bedwars.lobby.sync;

public enum SyncEventType {
    BLOCK_PLACE,
    BLOCK_BREAK,
    NPC_CREATE,
    NPC_DELETE,
    NPC_UPDATE,
    HOLOGRAM_CREATE,
    HOLOGRAM_DELETE,
    HOLOGRAM_UPDATE,
    CHUNK_SNAPSHOT,
    CONFIG_PUSH,
    FULL_SYNC,
    WORLD_SYNC,
    PARKOUR_SYNC;

    private static final SyncEventType[] VALUES = values();

    public static SyncEventType fromOrdinal(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }
}
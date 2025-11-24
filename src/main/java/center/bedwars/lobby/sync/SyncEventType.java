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
    WORLD_SYNC,
    PARKOUR_SYNC,
    FULL_SYNC
}
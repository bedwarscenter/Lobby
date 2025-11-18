package center.bedwars.lobby.sync;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SyncEventType {

    BLOCK_PLACE("block_place"),
    BLOCK_BREAK("block_break"),
    NPC_CREATE("npc_create"),
    NPC_DELETE("npc_delete"),
    NPC_UPDATE("npc_update"),
    HOLOGRAM_CREATE("hologram_create"),
    HOLOGRAM_DELETE("hologram_delete"),
    HOLOGRAM_UPDATE("hologram_update"),
    CHUNK_SNAPSHOT("chunk_snapshot"),
    CONFIG_PUSH("config_push"),
    FULL_SYNC("full_sync"),
    WORLD_SYNC("world_sync"),
    PARKOUR_SYNC("parkour_sync");

    private final String identifier;

    public static SyncEventType fromIdentifier(String identifier) {
        for (SyncEventType type : values()) {
            if (type.identifier.equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        return null;
    }
}
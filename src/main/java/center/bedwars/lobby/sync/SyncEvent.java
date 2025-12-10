package center.bedwars.lobby.sync;

import lombok.Getter;

@Getter
public class SyncEvent {
    private String lobbyId;
    private SyncEventType type;
    private byte[] data;
    private long timestamp;

    public SyncEvent() {}

    public SyncEvent(String lobbyId, SyncEventType type, byte[] data) {
        this.lobbyId = lobbyId;
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isFromSameLobby(String currentLobbyId) {
        return this.lobbyId.equals(currentLobbyId);
    }
}

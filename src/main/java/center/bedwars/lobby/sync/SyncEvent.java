package center.bedwars.lobby.sync;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncEvent {

    private String sourceLobby;
    private SyncEventType type;
    private long timestamp;
    private JsonObject data;

    public SyncEvent(String sourceLobby, SyncEventType type, JsonObject data) {
        this.sourceLobby = sourceLobby;
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isFromSameLobby(String currentLobbyId) {
        return sourceLobby != null && sourceLobby.equals(currentLobbyId);
    }
}
package center.bedwars.lobby.sync;

import center.bedwars.lobby.service.IService;

public interface ILobbySyncService extends IService {
    void syncLobby();

    void requestSync();

    void broadcastEvent(SyncEventType type, byte[] data);

    void performFullSync();
}

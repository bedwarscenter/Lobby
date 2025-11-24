package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;

public class ConfigSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        center.bedwars.lobby.configuration.ConfigurationManager.reloadConfigurationsPreservingLobbyId();
    }
}
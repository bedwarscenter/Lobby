package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.sync.SyncEvent;

public class ConfigSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        ConfigurationManager.reloadConfigurationsPreservingLobbyId();
    }
}
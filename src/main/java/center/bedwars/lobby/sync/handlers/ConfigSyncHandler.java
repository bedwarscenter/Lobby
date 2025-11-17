package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.sync.SyncEvent;
import com.google.gson.JsonObject;

public class ConfigSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();

        if (!data.has("configType")) {
            return;
        }

        String configType = data.get("configType").getAsString();

        Lobby.getINSTANCE().getLogger().info(
                "Received config sync for: " + configType
        );

        long reloadTime = ConfigurationManager.reloadConfigurationsPreservingLobbyId();

        Lobby.getINSTANCE().getLogger().info(
                String.format("Configurations reloaded in %dms (LOBBY_ID preserved)", reloadTime)
        );
    }
}
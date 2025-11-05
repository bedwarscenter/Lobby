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
                "Received config push for: " + configType
        );

        long reloadTime = ConfigurationManager.reloadConfigurations();

        Lobby.getINSTANCE().getLogger().info(
                String.format("Configurations reloaded in %dms", reloadTime)
        );
    }
}
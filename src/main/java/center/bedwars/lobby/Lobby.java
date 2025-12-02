package center.bedwars.lobby;

import center.bedwars.lobby.command.CommandManager;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.expansion.PlayerExpansion;
import center.bedwars.lobby.listener.ListenerManager;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.manager.ManagerStorage;
import center.bedwars.lobby.nametag.NametagManager;
import center.bedwars.lobby.nms.NMSManager;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.scoreboard.ScoreboardManager;
import center.bedwars.lobby.tablist.TablistManager;
import center.bedwars.lobby.sync.LobbySyncManager;
import center.bedwars.lobby.sync.EntityPlayerSyncManager;
import center.bedwars.lobby.sync.PlayerSyncManager;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import center.bedwars.lobby.manager.orphans.PlayerVisibilityManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lobby extends JavaPlugin {

    @Getter
    private static Lobby INSTANCE;

    @Getter
    private static ManagerStorage managerStorage;

    private PlayerExpansion playerExpansion;

    @Override
    public void onEnable() {
        INSTANCE = this;
        managerStorage = new ManagerStorage();

        managerStorage.registerAndLoad(new ConfigurationManager());
        managerStorage.registerAndLoad(new DependencyManager());

        SettingsConfiguration.LOBBY_ID = managerStorage
                .getManager(DependencyManager.class)
                .getPhoenix()
                .getApi()
                .getNetworkHandler()
                .getServerName();

        managerStorage.registerAndLoad(new NMSManager());
        managerStorage.registerAndLoad(new DatabaseManager());

        managerStorage.registerAndLoad(new CommandManager());
        managerStorage.registerAndLoad(new PlayerVisibilityManager());
        managerStorage.registerAndLoad(new HotbarManager());
        managerStorage.registerAndLoad(new ParkourManager());

        managerStorage.registerAndLoad(new ScoreboardManager());
        managerStorage.registerAndLoad(new TablistManager());
        managerStorage.registerAndLoad(new NametagManager());

        managerStorage.registerAndLoad(new PlayerSyncManager());
        managerStorage.registerAndLoad(new EntityPlayerSyncManager());
        managerStorage.registerAndLoad(new LobbySyncManager());

        managerStorage.registerAndLoad(new ListenerManager());

        ConfigurationManager.saveConfigurations();
        ConfigurationManager.reloadConfigurations();

        getLogger().info("BedWarsLobby enabled successfully!");
        if (managerStorage.getManager(DependencyManager.class).getPlaceholderAPI().isPresent()) {
            this.playerExpansion = new PlayerExpansion();
            playerExpansion.register();
        }
    }

    @Override
    public void onDisable() {
        if (managerStorage.getManager(DependencyManager.class).getPlaceholderAPI() != null && managerStorage.getManager(DependencyManager.class).getPlaceholderAPI().isPresent()) {
            if (playerExpansion != null) {
                playerExpansion.unregister();
            }
        }
        if (managerStorage != null) {
            managerStorage.unloadAll();
        }

        getLogger().info("BedWarsLobby disabled!");
    }
}
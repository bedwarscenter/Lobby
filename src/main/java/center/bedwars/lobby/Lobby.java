package center.bedwars.lobby;

import center.bedwars.lobby.command.ICommandService;
import center.bedwars.lobby.configuration.IConfigurationService;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.constant.ChannelConstants;
import center.bedwars.lobby.constant.LogMessages;
import center.bedwars.lobby.database.IDatabaseService;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.expansion.PlayerExpansion;
import center.bedwars.lobby.hotbar.IHotbarService;
import center.bedwars.lobby.injection.LobbyModule;
import center.bedwars.lobby.injection.ServiceManager;
import center.bedwars.lobby.listener.IListenerService;
import center.bedwars.lobby.menu.IMenuService;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.scoreboard.IScoreboardService;
import center.bedwars.lobby.sync.ILobbySyncService;
import center.bedwars.lobby.sync.IPlayerSyncService;
import center.bedwars.lobby.tablist.ITablistService;
import center.bedwars.lobby.visibility.IPlayerVisibilityService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lobby extends JavaPlugin {

    @Getter
    private static Lobby instance;

    @Getter
    private static Injector injector;

    @Getter
    private static ServiceManager serviceManager;

    private PlayerExpansion playerExpansion;

    @Override
    public void onEnable() {
        instance = this;
        injector = Guice.createInjector(new LobbyModule(this));
        serviceManager = new ServiceManager(injector, getLogger());

        serviceManager.enable(IConfigurationService.class);
        serviceManager.enable(IDependencyService.class);

        SettingsConfiguration.LOBBY_ID = serviceManager
                .get(IDependencyService.class)
                .getPhoenix()
                .getApi()
                .getNetworkHandler()
                .getServerName();

        serviceManager.enable(IDatabaseService.class);

        serviceManager.enable(ICommandService.class);
        serviceManager.enable(IPlayerVisibilityService.class);
        serviceManager.enable(IHotbarService.class);
        serviceManager.enable(IParkourService.class);
        serviceManager.enable(IMenuService.class);

        serviceManager.enable(IScoreboardService.class);
        serviceManager.enable(ITablistService.class);
        serviceManager.enable(INametagService.class);

        serviceManager.enable(IPlayerSyncService.class);
        serviceManager.enable(ILobbySyncService.class);

        serviceManager.enable(IListenerService.class);

        IConfigurationService configService = serviceManager.get(IConfigurationService.class);
        configService.saveConfigurations();
        configService.reloadConfigurations();

        getLogger().info(LogMessages.PLUGIN_ENABLED);

        if (serviceManager.get(IDependencyService.class).getPlaceholderAPI().isPresent()) {
            this.playerExpansion = new PlayerExpansion();
            playerExpansion.register();
        }

        getServer().getMessenger().registerOutgoingPluginChannel(this, ChannelConstants.BUNGEECORD);
    }

    @Override
    public void onDisable() {
        IDependencyService depService = serviceManager.get(IDependencyService.class);
        if (depService.getPlaceholderAPI() != null && depService.getPlaceholderAPI().isPresent()) {
            if (playerExpansion != null) {
                playerExpansion.unregister();
            }
        }

        if (serviceManager != null) {
            serviceManager.disableAll();
        }

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, ChannelConstants.BUNGEECORD);

        getLogger().info(LogMessages.PLUGIN_DISABLED);
    }
}

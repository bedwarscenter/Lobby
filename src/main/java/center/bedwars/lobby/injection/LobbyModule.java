package center.bedwars.lobby.injection;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.command.CommandService;
import center.bedwars.lobby.command.ICommandService;
import center.bedwars.lobby.configuration.ConfigurationService;
import center.bedwars.lobby.configuration.IConfigurationService;
import center.bedwars.lobby.database.DatabaseService;
import center.bedwars.lobby.database.IDatabaseService;
import center.bedwars.lobby.database.IMongoService;
import center.bedwars.lobby.database.IRedisService;
import center.bedwars.lobby.database.MongoService;
import center.bedwars.lobby.database.RedisService;
import center.bedwars.lobby.dependency.DependencyService;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.hotbar.HotbarService;
import center.bedwars.lobby.hotbar.IHotbarService;
import center.bedwars.lobby.listener.IListenerService;
import center.bedwars.lobby.listener.ListenerService;
import center.bedwars.lobby.menu.IMenuService;
import center.bedwars.lobby.menu.MenuService;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.nametag.NametagService;
import center.bedwars.lobby.nms.INMSService;
import center.bedwars.lobby.nms.NMSService;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.parkour.ParkourService;
import center.bedwars.lobby.scoreboard.IScoreboardService;
import center.bedwars.lobby.scoreboard.ScoreboardService;
import center.bedwars.lobby.sync.ILobbySyncService;
import center.bedwars.lobby.sync.IPlayerSyncService;
import center.bedwars.lobby.sync.LobbySyncService;
import center.bedwars.lobby.sync.PlayerSyncService;
import center.bedwars.lobby.sync.fake.FakePlayerManager;
import center.bedwars.lobby.tablist.ITablistService;
import center.bedwars.lobby.tablist.TablistService;
import center.bedwars.lobby.visibility.IPlayerVisibilityService;
import center.bedwars.lobby.visibility.PlayerVisibilityService;
import center.bedwars.lobby.snow.ISnowService;
import center.bedwars.lobby.snow.SnowService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class LobbyModule extends AbstractModule {

    private final Lobby plugin;

    public LobbyModule(Lobby plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Lobby.class).toInstance(plugin);

        bind(IConfigurationService.class).to(ConfigurationService.class).in(Singleton.class);
        bind(IDependencyService.class).to(DependencyService.class).in(Singleton.class);

        bind(IRedisService.class).to(RedisService.class).in(Singleton.class);
        bind(IMongoService.class).to(MongoService.class).in(Singleton.class);
        bind(IDatabaseService.class).to(DatabaseService.class).in(Singleton.class);

        bind(INMSService.class).to(NMSService.class).in(Singleton.class);
        bind(IHotbarService.class).to(HotbarService.class).in(Singleton.class);
        bind(IPlayerVisibilityService.class).to(PlayerVisibilityService.class).in(Singleton.class);
        bind(IMenuService.class).to(MenuService.class).in(Singleton.class);
        bind(IParkourService.class).to(ParkourService.class).in(Singleton.class);

        bind(IScoreboardService.class).to(ScoreboardService.class).in(Singleton.class);
        bind(ITablistService.class).to(TablistService.class).in(Singleton.class);
        bind(INametagService.class).to(NametagService.class).in(Singleton.class);

        bind(FakePlayerManager.class).in(Singleton.class);
        bind(IPlayerSyncService.class).to(PlayerSyncService.class).in(Singleton.class);
        bind(ILobbySyncService.class).to(LobbySyncService.class).in(Singleton.class);

        bind(ICommandService.class).to(CommandService.class).in(Singleton.class);
        bind(IListenerService.class).to(ListenerService.class).in(Singleton.class);

        bind(ISnowService.class).to(SnowService.class).in(Singleton.class);
    }
}

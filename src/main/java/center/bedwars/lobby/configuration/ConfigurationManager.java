package center.bedwars.lobby.configuration;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.*;
import center.bedwars.lobby.manager.Manager;
import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;

@SuppressWarnings({"unused"})
public class ConfigurationManager extends Manager {

    private final Lobby lobby = Lobby.getINSTANCE();
    private static ConfigHandler configHandler;
    private SettingsConfiguration settings;
    private LanguageConfiguration languageConfiguration;
    private SoundConfiguration soundConfiguration;
    private ItemsConfiguration itemsConfiguration;
    private ScoreboardConfiguration scoreboardConfiguration;
    private TablistConfiguration tablistConfiguration;

    @Override
    protected void onLoad() {
        File folder = lobby.getDataFolder();
        configHandler = new ConfigHandler(lobby.getLogger());

        this.settings = new SettingsConfiguration(folder, configHandler);
        this.settings.load();

        this.languageConfiguration = new LanguageConfiguration(folder, configHandler);
        this.languageConfiguration.load();

        this.soundConfiguration = new SoundConfiguration(folder, configHandler);
        this.soundConfiguration.load();

        this.itemsConfiguration = new ItemsConfiguration(folder, configHandler);
        this.itemsConfiguration.load();

        this.tablistConfiguration = new TablistConfiguration(folder, configHandler);
        this.tablistConfiguration.load();

        this.scoreboardConfiguration = new ScoreboardConfiguration(folder, configHandler);
        this.scoreboardConfiguration.load();
    }

    @Override
    protected void onUnload() {
        settings.load();
        languageConfiguration.load();
        soundConfiguration.load();
        itemsConfiguration.load();
    }

    public static long reloadConfigurations() {
        long start = System.currentTimeMillis();

        for (StaticConfig config : configHandler.getRegistered()) {
            try {
                config.load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return System.currentTimeMillis() - start;
    }

    public static long saveConfigurations() {
        long start = System.currentTimeMillis();

        configHandler.getRegistered().forEach(StaticConfig::save);

        return System.currentTimeMillis() - start;
    }

    public static long reloadConfigurationsPreservingLobbyId() {
        long start = System.currentTimeMillis();

        String preservedLobbyId = SettingsConfiguration.LOBBY_ID;

        for (StaticConfig config : configHandler.getRegistered()) {
            try {
                config.load();
            } catch (Exception e) {
                Lobby.getINSTANCE().getLogger().severe("Failed to load config: " + config.getClass().getSimpleName());
                e.printStackTrace();
            }
        }

        SettingsConfiguration.LOBBY_ID = preservedLobbyId;

        Lobby.getINSTANCE().getLogger().info(
                "Config sync completed - LOBBY_ID preserved: " + preservedLobbyId
        );

        return System.currentTimeMillis() - start;
    }

}
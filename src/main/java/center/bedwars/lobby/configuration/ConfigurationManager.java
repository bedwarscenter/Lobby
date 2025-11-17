package center.bedwars.lobby.configuration;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.ItemsConfiguration;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.configuration.configurations.SoundConfiguration;
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

        configHandler.getRegistered().forEach(StaticConfig::load);

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

        configHandler.getRegistered().forEach(StaticConfig::load);

        SettingsConfiguration.LOBBY_ID = preservedLobbyId;

        Lobby.getINSTANCE().getLogger().info(
                "Config sync completed - LOBBY_ID preserved: " + preservedLobbyId
        );

        return System.currentTimeMillis() - start;
    }

}
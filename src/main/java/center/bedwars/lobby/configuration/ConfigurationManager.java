package center.bedwars.lobby.configuration;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.manager.Manager;
import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;

@SuppressWarnings({"unused"})
public class ConfigurationManager extends Manager {

    private final Lobby lobby = Lobby.getINSTANCE();
    private static ConfigHandler configHandler;
    private SettingsConfiguration settings;

    @Override
    protected void onLoad() {
        File folder = lobby.getDataFolder();
        configHandler = new ConfigHandler(lobby.getLogger());
        this.settings = new SettingsConfiguration(folder, configHandler);
        this.settings.load();
    }

    @Override
    protected void onUnload() {
        settings.load();
    }

    public static long reloadConfigurations() {
        long start = System.currentTimeMillis();

        configHandler.getRegistered().forEach(StaticConfig::load);

        return System.currentTimeMillis() - start;
    }
}

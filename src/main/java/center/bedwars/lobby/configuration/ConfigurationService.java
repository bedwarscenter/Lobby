package center.bedwars.lobby.configuration;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.*;
import center.bedwars.lobby.configuration.providers.GroupConfigProvider;
import center.bedwars.lobby.service.AbstractService;
import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;

@Singleton
public class ConfigurationService extends AbstractService implements IConfigurationService {

    private final Lobby lobby;
    private ConfigHandler configHandler;
    private SettingsConfiguration settings;
    private LanguageConfiguration languageConfiguration;
    private SoundConfiguration soundConfiguration;
    private ItemsConfiguration itemsConfiguration;
    private ScoreboardConfiguration scoreboardConfiguration;
    private TablistConfiguration tablistConfiguration;
    private NametagConfiguration nametagConfiguration;
    private MenuConfiguration menuConfiguration;

    @Inject
    public ConfigurationService(Lobby lobby) {
        this.lobby = lobby;
    }

    @Override
    protected void onEnable() {
        File folder = lobby.getDataFolder();
        configHandler = new ConfigHandler(lobby.getLogger());

        registerProviders();

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

        this.nametagConfiguration = new NametagConfiguration(folder, configHandler);
        this.nametagConfiguration.load();

        this.menuConfiguration = new MenuConfiguration(folder, configHandler);
        this.menuConfiguration.load();
    }

    @Override
    protected void onDisable() {
        saveConfigurations();
    }

    private void registerProviders() {
        configHandler.bind(NametagConfiguration.GroupConfig.class,
                new GroupConfigProvider.NametagGroupConfigProvider());
    }

    @Override
    public long reloadConfigurations() {
        long start = System.currentTimeMillis();

        for (StaticConfig config : configHandler.getRegistered()) {
            try {
                config.load();
            } catch (Exception e) {
                lobby.getLogger().severe("Failed to reload config: " + config.getClass().getSimpleName());
                e.printStackTrace();
            }
        }

        return System.currentTimeMillis() - start;
    }

    @Override
    public long saveConfigurations() {
        long start = System.currentTimeMillis();

        for (StaticConfig config : configHandler.getRegistered()) {
            try {
                config.save();
            } catch (Exception e) {
                lobby.getLogger().severe("Failed to save config: " + config.getClass().getSimpleName());
                e.printStackTrace();
            }
        }

        return System.currentTimeMillis() - start;
    }

    @Override
    public void reload() {
        reloadConfigurations();
    }

    @Override
    public void save() {
        saveConfigurations();
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }
}

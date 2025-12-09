package center.bedwars.lobby.command;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.command.commands.*;
import center.bedwars.lobby.service.AbstractService;
import net.j4c0b3y.api.command.bukkit.BukkitCommandHandler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class CommandService extends AbstractService implements ICommandService {

    private final Lobby plugin;
    private final Injector injector;
    private BukkitCommandHandler commandHandler;

    @Inject
    public CommandService(Lobby plugin, Injector injector) {
        this.plugin = plugin;
        this.injector = injector;
    }

    @Override
    protected void onEnable() {
        registerCommands();
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void registerCommands() {
        this.commandHandler = new BukkitCommandHandler(plugin);

        commandHandler.register(injector.getInstance(BedWarsLobbyCommand.class));
        commandHandler.register(injector.getInstance(SpawnCommand.class));
        commandHandler.register(injector.getInstance(ParkourCommand.class));
        commandHandler.register(injector.getInstance(SyncCommand.class));
        commandHandler.register(injector.getInstance(CosmeticsCommand.class));
        commandHandler.register(injector.getInstance(CollectiblesCommand.class));

        // Register togglesnow command via Bukkit API
        org.bukkit.command.PluginCommand snowCmd = plugin.getCommand("togglesnow");
        if (snowCmd != null) {
            snowCmd.setExecutor(injector.getInstance(ToggleSnowCommand.class));
        }
    }
}

package center.bedwars.lobby;

import center.bedwars.lobby.command.CommandManager;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.listener.ListenerManager;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.manager.ManagerStorage;
import center.bedwars.lobby.nms.NMSManager;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.LobbySyncManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lobby extends JavaPlugin {

    @Getter
    private static Lobby INSTANCE;

    @Getter
    private static ManagerStorage managerStorage;

    @Override
    public void onEnable() {
        INSTANCE = this;
        managerStorage = new ManagerStorage();

        managerStorage.registerAndLoad(new ConfigurationManager());
        managerStorage.registerAndLoad(new NMSManager());
        managerStorage.registerAndLoad(new DependencyManager());
        managerStorage.registerAndLoad(new ParkourManager());
        managerStorage.registerAndLoad(new CommandManager());
        managerStorage.registerAndLoad(new ListenerManager());
        managerStorage.registerAndLoad(new LobbySyncManager());

        managerStorage.setAllWaiting();
        managerStorage.finishAll();
    }

    @Override
    public void onDisable() {
        if (managerStorage != null) {
            managerStorage.unloadAll();
        }
    }

}
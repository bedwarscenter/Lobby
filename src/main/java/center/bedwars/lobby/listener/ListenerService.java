package center.bedwars.lobby.listener;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.listener.listeners.general.*;
import center.bedwars.lobby.listener.listeners.hotbar.HotbarListener;
import center.bedwars.lobby.listener.listeners.important.*;
import center.bedwars.lobby.listener.listeners.parkour.*;

import center.bedwars.lobby.listener.listeners.sync.*;
import center.bedwars.lobby.service.AbstractService;
import com.google.inject.Injector;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ListenerService extends AbstractService implements IListenerService {

    private final Lobby plugin;
    private final Injector injector;
    private final List<Listener> listeners = new ArrayList<>();

    @Inject
    public ListenerService(Lobby plugin, Injector injector) {
        this.plugin = plugin;
        this.injector = injector;
    }

    @Override
    protected void onEnable() {
        registerListeners();
    }

    @Override
    protected void onDisable() {
        listeners.clear();
    }

    @Override
    public void registerListeners() {
        register(injector.getInstance(JoinListener.class));
        register(injector.getInstance(LevelChangeListener.class));
        register(injector.getInstance(QuitListener.class));
        register(injector.getInstance(MoveListener.class));
        register(injector.getInstance(HotbarListener.class));
        register(injector.getInstance(ParkourMoveListener.class));
        register(injector.getInstance(ParkourListener.class));
        register(injector.getInstance(WorldWeatherListener.class));
        register(injector.getInstance(WorldDayListener.class));
        register(injector.getInstance(PlayerEnvironmentListener.class));
        register(injector.getInstance(PlayerSafetyListener.class));
        register(injector.getInstance(PlayerRestrictionListener.class));
        register(injector.getInstance(HologramCreationListener.class));
        register(injector.getInstance(PlayerSyncListener.class));

        register(injector.getInstance(EntityPacketListener.class));
    }

    private void register(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }
}

package center.bedwars.lobby.listener;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.listener.listeners.general.*;
import center.bedwars.lobby.listener.listeners.hotbar.HotbarListener;
import center.bedwars.lobby.listener.listeners.important.*;
import center.bedwars.lobby.listener.listeners.parkour.*;
import center.bedwars.lobby.listener.listeners.phoenix.DisguiseListener;
import center.bedwars.lobby.listener.listeners.sync.*;
import center.bedwars.lobby.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager extends Manager {

    private final List<Listener> listeners = new ArrayList<>();

    @Override
    protected void onLoad() {
        registerListeners(
                new JoinListener(),
                new QuitListener(),
                new MoveListener(),
                new HotbarListener(),
                new ParkourMoveListener(),
                new ParkourListener(),
                new WorldWeatherListener(),
                new WorldDayListener(),
                new WorldChangeListener(),
                new PlayerEnvironmentListener(),
                new PlayerSafetyListener(),
                new PlayerRestrictionListener(),
                new LobbySyncListener(),
                new NPCCreationListener(),
                new PlayerSyncListener(),
                new EntityPacketListener(),
                new HologramCreationListener(),
                new DisguiseListener()
        );
    }

    @Override
    protected void onUnload() {
        listeners.clear();
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, Lobby.getINSTANCE());
            this.listeners.add(listener);
        }
    }
}
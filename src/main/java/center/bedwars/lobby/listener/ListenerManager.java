package center.bedwars.lobby.listener;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.listener.listeners.general.PlayerEnvironmentListener;
import center.bedwars.lobby.listener.listeners.general.PlayerRestrictionListener;
import center.bedwars.lobby.listener.listeners.general.PlayerSafetyListener;
import center.bedwars.lobby.listener.listeners.general.WorldDayListener;
import center.bedwars.lobby.listener.listeners.general.WorldWeatherListener;
import center.bedwars.lobby.listener.listeners.hotbar.HotbarListener;
import center.bedwars.lobby.listener.listeners.important.JoinListener;
import center.bedwars.lobby.listener.listeners.important.QuitListener;
import center.bedwars.lobby.listener.listeners.parkour.MoveListener;
import center.bedwars.lobby.listener.listeners.parkour.ParkourListener;
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
                new WorldWeatherListener(),
                new WorldDayListener(),
                new ParkourListener(),
                new HotbarListener(),
                new LobbySyncListener(),
                new NPCCreationListener(),
                new PlayerSyncListener(),
                new EntityPacketListener(),
                new HologramCreationListener(),
                new PlayerEnvironmentListener(),
                new PlayerSafetyListener(),
                new PlayerRestrictionListener()
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
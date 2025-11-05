package center.bedwars.lobby.listener;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.listener.listeners.important.JoinListener;
import center.bedwars.lobby.listener.listeners.important.QuitListener;
import center.bedwars.lobby.listener.listeners.parkour.MoveListener;
import center.bedwars.lobby.listener.listeners.sync.LobbySyncListener;
import center.bedwars.lobby.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager extends Manager {

    private final List<Listener> listeners = new ArrayList<>();

    @Override
    protected void onLoad() throws Exception {
        registerListener(new JoinListener());
        registerListener(new QuitListener());
        registerListener(new MoveListener());
        registerListener(new LobbySyncListener());
    }

    @Override
    protected void onUnload() throws Exception {
        listeners.clear();
    }

    private void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, Lobby.getINSTANCE());
        listeners.add(listener);
    }
}
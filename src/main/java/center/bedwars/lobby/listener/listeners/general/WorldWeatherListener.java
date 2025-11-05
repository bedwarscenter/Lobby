package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.Lobby;
import org.bukkit.event.Listener;

public class WorldWeatherListener implements Listener {

    private Lobby lobby;

    public WorldWeatherListener() {
        lobby = Lobby.getINSTANCE();
    }
    
}

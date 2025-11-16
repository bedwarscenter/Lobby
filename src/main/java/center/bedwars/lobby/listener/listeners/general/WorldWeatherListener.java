package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.Lobby;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WorldWeatherListener implements Listener {

    private Lobby lobby;

    public WorldWeatherListener() {
        lobby = Lobby.getINSTANCE();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
        World world = event.getWorld();
        world.setThundering(false);
        world.setStorm(false);
        world.setWeatherDuration(0);

    }

}

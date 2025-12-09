package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.Lobby;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldDayListener implements Listener {

    private final Lobby lobby;

    public WorldDayListener() {
        this.lobby = Lobby.getInstance();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();

        new BukkitRunnable() {
            @Override
            public void run() {
                world.setTime(1000);
            }
        }.runTaskTimer(lobby, 0L, 20L);
    }

}

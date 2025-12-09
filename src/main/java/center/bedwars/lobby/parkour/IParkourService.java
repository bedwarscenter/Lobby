package center.bedwars.lobby.parkour;

import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.service.IService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface IParkourService extends IService {
    void startParkour(Player player, Parkour parkour);

    boolean leaveParkour(Player player, boolean teleportToSpawn);

    void quitParkour(Player player);

    void resetPlayer(Player player);

    void teleportToCheckpoint(Player player);

    void handleCheckpoint(Player player, Location location);

    void handleFinish(Player player, Location location);

    void handlePlayerQuit(Player player);

    boolean hasActiveSession(Player player);

    ParkourSession getSession(Player player);

    Parkour getParkourAtLocation(Location location);

    void refreshParkours();

    void handleItemClick(Player player, org.bukkit.inventory.ItemStack item);
}

package center.bedwars.lobby.snow;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface ISnowService extends IService {
    void toggleSnow(Player player);

    boolean hasSnowEnabled(UUID playerUuid);

    void onPlayerJoin(Player player);

    void onPlayerQuit(Player player);

    void onPlayerMove(Player player);
}

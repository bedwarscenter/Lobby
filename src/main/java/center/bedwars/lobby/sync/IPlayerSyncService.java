package center.bedwars.lobby.sync;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;

public interface IPlayerSyncService extends IService {
    void handlePlayerJoin(Player player);

    void handlePlayerQuit(Player player);

    void broadcast(Player player, center.bedwars.lobby.sync.serialization.PlayerSerializer.PlayerSyncAction action,
            String data);
}

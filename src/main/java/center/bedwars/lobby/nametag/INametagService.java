package center.bedwars.lobby.nametag;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;

public interface INametagService extends IService {
    void createNametag(Player player);

    void removeNametag(Player player);

    void updateNametag(Player player);

    void reload();
}

package center.bedwars.lobby.tablist;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;

public interface ITablistService extends IService {
    void createTablist(Player player);

    void removeTablist(Player player);

    void updateTablist(Player player);

    void reload();
}

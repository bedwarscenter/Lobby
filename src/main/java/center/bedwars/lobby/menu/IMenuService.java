package center.bedwars.lobby.menu;

import center.bedwars.lobby.service.IService;
import net.j4c0b3y.api.menu.Menu;
import org.bukkit.entity.Player;

public interface IMenuService extends IService {

    void openQuickplayMenu(Player player);

    void openYourProfileMenu(Player player);

    void openBedWarsMenu(Player player);

    void openLobbySelectorMenu(Player player);

}

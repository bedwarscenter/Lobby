package center.bedwars.lobby.menu.menus;

import net.j4c0b3y.api.menu.Menu;
import net.j4c0b3y.api.menu.MenuSize;
import net.j4c0b3y.api.menu.layer.impl.BackgroundLayer;
import net.j4c0b3y.api.menu.layer.impl.ForegroundLayer;
import org.bukkit.entity.Player;

public class BedWarsMenu extends Menu {

    public BedWarsMenu(Player player) {
        super("BedWars", MenuSize.SIX, player);
    }

    @Override
    public void setup(BackgroundLayer background, ForegroundLayer foreground) {
    }

    @Override
    public void onClose() {
    }
}

package center.bedwars.lobby.menu;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.menu.menus.BedWarsMenu;
import center.bedwars.lobby.menu.menus.LobbySelectorMenu;
import center.bedwars.lobby.menu.menus.QuickPlayMenu;
import center.bedwars.lobby.menu.menus.YourProfileMenu;
import center.bedwars.lobby.service.AbstractService;
import center.bedwars.lobby.util.ServerTransferUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.j4c0b3y.api.menu.MenuHandler;
import org.bukkit.entity.Player;

@Singleton
@SuppressWarnings("unused")
public class MenuService extends AbstractService implements IMenuService {

    private final Lobby plugin;
    private final IDependencyService dependencyService;
    private final ServerTransferUtil transferUtil;
    private MenuHandler menuHandler;

    @Inject
    public MenuService(Lobby plugin, IDependencyService dependencyService, ServerTransferUtil transferUtil) {
        this.plugin = plugin;
        this.dependencyService = dependencyService;
        this.transferUtil = transferUtil;
    }

    @Override
    protected void onEnable() {
        this.menuHandler = new MenuHandler(plugin);
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void openQuickplayMenu(Player player) {
        new QuickPlayMenu(player).open();
    }

    @Override
    public void openYourProfileMenu(Player player) {
        new YourProfileMenu(player).open();
    }

    @Override
    public void openBedWarsMenu(Player player) {
        new BedWarsMenu(player).open();
    }

    @Override
    public void openLobbySelectorMenu(Player player) {
        new LobbySelectorMenu(player, dependencyService, transferUtil).open();
    }
}

package center.bedwars.lobby.hotbar;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface IHotbarService extends IService {
    void giveLobbyHotbar(Player player);

    void giveParkourHotbar(Player player);

    void clearHotbar(Player player);

    boolean isLobbyItem(ItemStack item);

    boolean isParkourItem(ItemStack item);

    void updateHotbar(Player player);
}

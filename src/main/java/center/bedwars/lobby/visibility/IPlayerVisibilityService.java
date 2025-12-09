package center.bedwars.lobby.visibility;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface IPlayerVisibilityService extends IService {
    void handlePlayerJoin(Player player);

    void handlePlayerQuit(Player player);

    void setVisibility(Player player, boolean visible);

    boolean hasVisibilityEnabled(Player player);

    void toggleVisibility(Player player);

    boolean toggleVisibilityWithCooldown(Player player);

    long getRemainingCooldown(Player player);

    boolean isHidden(Player player);

    CompletableFuture<Void> loadVisibilityAsync(Player player);

    CompletableFuture<Void> saveVisibilityAsync(Player player);
}

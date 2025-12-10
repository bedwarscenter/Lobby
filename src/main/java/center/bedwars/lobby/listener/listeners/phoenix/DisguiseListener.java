package center.bedwars.lobby.listener.listeners.phoenix;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.hotbar.IHotbarService;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.tablist.ITablistService;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import xyz.refinedev.phoenix.utils.events.disguise.ProfileDisguiseEvent;
import xyz.refinedev.phoenix.utils.events.disguise.ProfileUndisguiseEvent;

public class DisguiseListener implements Listener {

    private final Lobby plugin;
    private final IHotbarService hotbarService;
    private final ITablistService tablistService;
    private final INametagService nametagService;

    @Inject
    public DisguiseListener(Lobby plugin, IHotbarService hotbarService, ITablistService tablistService,
            INametagService nametagService) {
        this.plugin = plugin;
        this.hotbarService = hotbarService;
        this.tablistService = tablistService;
        this.nametagService = nametagService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileDisguise(ProfileDisguiseEvent event) {
        Player player = Bukkit.getPlayer(event.getProfile().getUuid());
        if (player == null)
            return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (hotbarService != null) {
                hotbarService.updateHotbar(player);
            }
            updateAllDisplays();
        }, 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileUnDisguise(ProfileUndisguiseEvent event) {
        Player player = Bukkit.getPlayer(event.getProfile().getUuid());
        if (player == null)
            return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (hotbarService != null) {
                hotbarService.updateHotbar(player);
            }
            updateAllDisplays();
        }, 3L);
    }

    private void updateAllDisplays() {
        if (nametagService != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                nametagService.removeNametag(online);
                nametagService.createNametag(online);
            });
        }

        if (tablistService != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                tablistService.removeTablist(online);
                tablistService.createTablist(online);
            });
        }
    }
}

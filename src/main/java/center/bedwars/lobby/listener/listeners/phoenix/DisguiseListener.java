package center.bedwars.lobby.listener.listeners.phoenix;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import center.bedwars.lobby.nametag.NametagManager;
import center.bedwars.lobby.tablist.TablistManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import xyz.refinedev.phoenix.utils.events.disguise.ProfileDisguiseEvent;
import xyz.refinedev.phoenix.utils.events.disguise.ProfileUndisguiseEvent;

public class DisguiseListener implements Listener {

    private final HotbarManager hotbarManager;
    private final TablistManager tablistManager;
    private final NametagManager nametagManager;

    public DisguiseListener() {
        this.hotbarManager = Lobby.getManagerStorage().getManager(HotbarManager.class);
        this.tablistManager = Lobby.getManagerStorage().getManager(TablistManager.class);
        this.nametagManager = Lobby.getManagerStorage().getManager(NametagManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileDisguise(ProfileDisguiseEvent event) {
        Player player = Bukkit.getPlayer(event.getProfile().getUuid());
        if (player == null) return;

        hotbarManager.giveLobbyHotbar(player);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            updatePlayerDisplay(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileUnDisguise(ProfileUndisguiseEvent event) {
        Player player = Bukkit.getPlayer(event.getProfile().getUuid());
        if (player == null) return;

        hotbarManager.giveLobbyHotbar(player);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            updatePlayerDisplay(player);
        }, 1L);
    }

    private void updatePlayerDisplay(Player player) {
        if (tablistManager != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                tablistManager.removeTablist(online);
                tablistManager.createTablist(online);
            });
        }

        if (nametagManager != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                nametagManager.removeNametag(online);
                nametagManager.createNametag(online);
            });
        }
    }
}
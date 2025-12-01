package center.bedwars.lobby.listener.listeners.phoenix;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import xyz.refinedev.phoenix.utils.events.disguise.ProfileDisguiseEvent;
import xyz.refinedev.phoenix.utils.events.disguise.ProfileUndisguiseEvent;

public class DisguiseListener implements Listener {

    private HotbarManager hotbarManager;

    public DisguiseListener() {
        hotbarManager = Lobby.getManagerStorage().getManager(HotbarManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileDisguise(ProfileDisguiseEvent event) {
        Player player = Bukkit.getPlayer(event.getProfile().getPlayerName());
        hotbarManager.giveLobbyHotbar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProfileUnDisguise(ProfileUndisguiseEvent event) {
        Player player = Bukkit.getPlayer(event.getProfile().getPlayerName());
        hotbarManager.giveLobbyHotbar(player);
    }

}

package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.manager.orphans.PlayerVisibilityManager;
import center.bedwars.lobby.nametag.NametagManager;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.scoreboard.ScoreboardManager;
import center.bedwars.lobby.tablist.TablistManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final ParkourManager parkourManager;
    private final ScoreboardManager scoreboardManager;
    private final TablistManager tablistManager;
    private final NametagManager nametagManager;
    private final PlayerVisibilityManager visibilityManager;

    public QuitListener() {
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
        this.scoreboardManager = Lobby.getManagerStorage().getManager(ScoreboardManager.class);
        this.tablistManager = Lobby.getManagerStorage().getManager(TablistManager.class);
        this.nametagManager = Lobby.getManagerStorage().getManager(NametagManager.class);
        this.visibilityManager = Lobby.getManagerStorage().getManager(PlayerVisibilityManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        Player player = event.getPlayer();
        if (player == null) return;

        parkourManager.handlePlayerQuit(player);

        if (scoreboardManager != null) {
            scoreboardManager.removeScoreboard(player);
        }

        if (nametagManager != null) {
            nametagManager.removeNametag(player);
        }

        if (tablistManager != null) {
            tablistManager.removeTablist(player);
        }

        if (visibilityManager != null) {
            visibilityManager.handlePlayerQuit(player);
        }

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), this::updateRemainingPlayers, 5L);
    }

    private void updateRemainingPlayers() {
        if (nametagManager != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                nametagManager.removeNametag(online);
                nametagManager.createNametag(online);
            });
        }

        if (tablistManager != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                tablistManager.removeTablist(online);
                tablistManager.createTablist(online);
            });
        }
    }
}
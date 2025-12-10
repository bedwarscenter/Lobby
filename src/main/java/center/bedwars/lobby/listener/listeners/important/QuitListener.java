package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.scoreboard.IScoreboardService;
import center.bedwars.lobby.tablist.ITablistService;
import center.bedwars.lobby.visibility.IPlayerVisibilityService;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final Lobby plugin;
    private final IParkourService parkourService;
    private final IScoreboardService scoreboardService;
    private final ITablistService tablistService;
    private final INametagService nametagService;
    private final IPlayerVisibilityService visibilityService;

    @Inject
    public QuitListener(Lobby plugin, IParkourService parkourService, IScoreboardService scoreboardService,
            ITablistService tablistService, INametagService nametagService,
            IPlayerVisibilityService visibilityService) {
        this.plugin = plugin;
        this.parkourService = parkourService;
        this.scoreboardService = scoreboardService;
        this.tablistService = tablistService;
        this.nametagService = nametagService;
        this.visibilityService = visibilityService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        Player player = event.getPlayer();
        if (player == null)
            return;

        parkourService.handlePlayerQuit(player);

        if (scoreboardService != null) {
            scoreboardService.removeScoreboard(player);
        }

        if (nametagService != null) {
            nametagService.removeNametag(player);
        }

        if (tablistService != null) {
            tablistService.removeTablist(player);
        }

        if (visibilityService != null) {
            visibilityService.handlePlayerQuit(player);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::updateRemainingPlayers, 5L);
    }

    private void updateRemainingPlayers() {
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

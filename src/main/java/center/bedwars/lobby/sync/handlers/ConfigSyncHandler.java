package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.IConfigurationService;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.scoreboard.IScoreboardService;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.tablist.ITablistService;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConfigSyncHandler implements ISyncHandler {

    private final Lobby plugin;
    private final IConfigurationService configurationService;
    private final IScoreboardService scoreboardService;
    private final ITablistService tablistService;
    private final INametagService nametagService;

    @Inject
    public ConfigSyncHandler(Lobby plugin, IConfigurationService configurationService,
            IScoreboardService scoreboardService,
            ITablistService tablistService, INametagService nametagService) {
        this.plugin = plugin;
        this.configurationService = configurationService;
        this.scoreboardService = scoreboardService;
        this.tablistService = tablistService;
        this.nametagService = nametagService;
    }

    @Override
    public void handle(SyncEvent event) {
        configurationService.reloadConfigurations();

        Bukkit.getScheduler().runTask(plugin, () -> {

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (scoreboardService != null) {
                    scoreboardService.removeScoreboard(player);
                    scoreboardService.createScoreboard(player);
                }

                if (tablistService != null) {
                    tablistService.removeTablist(player);
                    tablistService.createTablist(player);
                }

                if (nametagService != null) {
                    nametagService.removeNametag(player);
                    nametagService.createNametag(player);
                }
            }
        });
    }
}

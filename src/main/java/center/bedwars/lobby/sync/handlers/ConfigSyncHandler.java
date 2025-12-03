package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.nametag.NametagManager;
import center.bedwars.lobby.scoreboard.ScoreboardManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.tablist.TablistManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConfigSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        center.bedwars.lobby.configuration.ConfigurationManager.reloadConfigurationsPreservingLobbyId();

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            ScoreboardManager scoreboardManager = Lobby.getManagerStorage().getManager(ScoreboardManager.class);
            TablistManager tablistManager = Lobby.getManagerStorage().getManager(TablistManager.class);
            NametagManager nametagManager = Lobby.getManagerStorage().getManager(NametagManager.class);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (scoreboardManager != null) {
                    scoreboardManager.removeScoreboard(player);
                    scoreboardManager.createScoreboard(player);
                }

                if (tablistManager != null) {
                    tablistManager.removeTablist(player);
                    tablistManager.createTablist(player);
                }

                if (nametagManager != null) {
                    nametagManager.removeNametag(player);
                    nametagManager.createNametag(player);
                }
            }
        });
    }
}
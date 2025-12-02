package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.nametag.NametagManager;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.scoreboard.ScoreboardManager;
import center.bedwars.lobby.tablist.TablistManager;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.refinedev.phoenix.rank.IRank;

public class JoinListener implements Listener {

    private final DependencyManager dependencyManager;
    private final ParkourManager parkourManager;
    private final ScoreboardManager scoreboardManager;
    private final TablistManager tablistManager;
    private final NametagManager nametagManager;

    public JoinListener() {
        this.dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
        this.scoreboardManager = Lobby.getManagerStorage().getManager(ScoreboardManager.class);
        this.tablistManager = Lobby.getManagerStorage().getManager(TablistManager.class);
        this.nametagManager = Lobby.getManagerStorage().getManager(NametagManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        if (player == null) return;

        parkourManager.handlePlayerQuit(player);

        if (SettingsConfiguration.PLAYER.TELEPORT_ON_JOIN) {
            SpawnUtil.teleportToSpawn(player);
        }

        if (scoreboardManager != null) {
            scoreboardManager.createScoreboard(player);
        }

        if (tablistManager != null) {
            tablistManager.createTablist(player);
        }

        if (nametagManager != null) {
            nametagManager.createNametag(player);
        }

        String joinMessage = this.getJoinMessage(player);
        if (joinMessage != null) {
            event.setJoinMessage(joinMessage);
        }
    }

    private String getJoinMessage(Player player) {
        try {
            if (dependencyManager == null) return null;

            PhoenixDependency phoenixDependency = dependencyManager.getPhoenix();
            if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) return null;

            IRank playerRank = phoenixDependency.getApi().getProfileHandler().getProfile(player.getUniqueId()).getHighestRank();
            if(playerRank == null) return null;

            String messsage = SettingsConfiguration.JOIN_MESSAGES.get(playerRank.getName());
            if (messsage == null || messsage.isEmpty()) return null;

            return ColorUtil.color(messsage
                    .replace("<player>", player.getName())
                    .replace("<rank>", playerRank.getName())
                    .replace("<prefix>", playerRank.getPrefix())
                    .replace("<color>", playerRank.getColorLegacy())
            );

        } catch (Exception e) {
            Lobby.getINSTANCE().getLogger().warning("Error processing join message for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
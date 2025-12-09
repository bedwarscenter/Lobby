package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.dependency.dependencies.AlonsoLevelsDependency;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.scoreboard.IScoreboardService;
import center.bedwars.lobby.tablist.ITablistService;
import center.bedwars.lobby.visibility.IPlayerVisibilityService;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import com.alonsoaliaga.alonsolevels.api.AlonsoLevelsAPI;
import com.alonsoaliaga.alonsolevels.others.PlayerData;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.refinedev.phoenix.rank.IRank;

public class JoinListener implements Listener {

    private final Lobby plugin;
    private final IDependencyService dependencyService;
    private final IParkourService parkourService;
    private final IScoreboardService scoreboardService;
    private final ITablistService tablistService;
    private final INametagService nametagService;
    private final IPlayerVisibilityService visibilityService;

    @Inject
    public JoinListener(Lobby plugin, IDependencyService dependencyService, IParkourService parkourService,
            IScoreboardService scoreboardService, ITablistService tablistService,
            INametagService nametagService, IPlayerVisibilityService visibilityService) {
        this.plugin = plugin;
        this.dependencyService = dependencyService;
        this.parkourService = parkourService;
        this.scoreboardService = scoreboardService;
        this.tablistService = tablistService;
        this.nametagService = nametagService;
        this.visibilityService = visibilityService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        if (player == null)
            return;

        parkourService.handlePlayerQuit(player);

        if (SettingsConfiguration.PLAYER.TELEPORT_ON_JOIN) {
            SpawnUtil.teleportToSpawn(player);
        }

        if (scoreboardService != null) {
            scoreboardService.createScoreboard(player);
        }

        if (nametagService != null) {
            nametagService.createNametag(player);
        }

        if (tablistService != null) {
            tablistService.createTablist(player);
        }

        if (visibilityService != null) {
            visibilityService.handlePlayerJoin(player);
        }

        setupAlonsoLevels(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateOtherPlayers(player), 5L);

        String joinMessage = this.getJoinMessage(player);
        if (joinMessage != null) {
            event.setJoinMessage(joinMessage);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> setupAlonsoLevels(player), 5L);
    }

    private void setupAlonsoLevels(Player player) {
        if (dependencyService == null)
            return;

        AlonsoLevelsDependency alonsoLevelsDependency = dependencyService.getAlonsoLevels();
        if (alonsoLevelsDependency == null || !alonsoLevelsDependency.isApiAvailable())
            return;

        try {
            if (!AlonsoLevelsAPI.isLoaded(player.getUniqueId())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> setupAlonsoLevels(player), 20L);
                return;
            }

            int level = AlonsoLevelsAPI.getLevel(player.getUniqueId());
            if (level != -1) {
                player.setLevel(level);
            }

            PlayerData playerData = com.alonsoaliaga.alonsolevels.AlonsoLevels.getInstance().getDataMap()
                    .get(player.getUniqueId());
            if (playerData != null) {
                int progressPercentage = com.alonsoaliaga.alonsolevels.AlonsoLevels.getInstance()
                        .getProgressPercentage(playerData);
                if (progressPercentage != -1) {
                    float expProgress = (float) progressPercentage / 100.0f;
                    player.setExp(Math.max(0.0f, Math.min(1.0f, expProgress)));
                }
            }
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Error loading AlonsoLevels data for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void updateOtherPlayers(Player newPlayer) {
        if (nametagService != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                if (!online.equals(newPlayer)) {
                    nametagService.removeNametag(online);
                    nametagService.createNametag(online);
                }
            });
        }

        if (tablistService != null) {
            Bukkit.getOnlinePlayers().forEach(online -> {
                tablistService.removeTablist(online);
                tablistService.createTablist(online);
            });
        }
    }

    private String getJoinMessage(Player player) {
        try {
            if (dependencyService == null)
                return null;

            PhoenixDependency phoenixDependency = dependencyService.getPhoenix();
            if (phoenixDependency == null || !phoenixDependency.isApiAvailable())
                return null;

            IRank playerRank = phoenixDependency.getApi().getProfileHandler().getProfile(player.getUniqueId())
                    .getHighestRank();
            if (playerRank == null)
                return null;

            String messsage = SettingsConfiguration.JOIN_MESSAGES.get(playerRank.getName());
            if (messsage == null || messsage.isEmpty())
                return null;

            return ColorUtil.color(messsage
                    .replace("<player>", player.getName())
                    .replace("<rank>", playerRank.getName())
                    .replace("<prefix>", playerRank.getPrefix())
                    .replace("<color>", playerRank.getColorLegacy()));

        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Error processing join message for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
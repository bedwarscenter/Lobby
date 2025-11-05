package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.refinedev.phoenix.rank.IRank;

public class JoinListener implements Listener {

    private final DependencyManager dependencyManager;
    private final ParkourManager parkourManager;

    public JoinListener() {
        this.dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        if (player == null) return;

        parkourManager.handlePlayerQuit(player);

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

            IRank playerRank = phoenixDependency.getApi().getGrantHandler().getHighestRank(player.getUniqueId());
            if(playerRank == null) return null;

            String messsage = SettingsConfiguration.JOIN_MESSAGES.get(player.getName());
            if (messsage == null || messsage.isEmpty()) return null;

            return ColorUtil.color(messsage
                    .replace("<player>", player.getName())
                    .replace("<rank>", playerRank.getName())
                    .replace("<color>", playerRank.getColorLegacy())
            );

        } catch (Exception e) {
            Lobby.getINSTANCE().getLogger().warning("Error processing join message for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
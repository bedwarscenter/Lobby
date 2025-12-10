package center.bedwars.lobby.listener.listeners.important;

import center.bedwars.lobby.Lobby;
import com.alonsoaliaga.alonsolevels.AlonsoLevels;
import com.alonsoaliaga.alonsolevels.api.AlonsoLevelsAPI;
import com.alonsoaliaga.alonsolevels.api.events.ExperienceChangeEvent;
import com.alonsoaliaga.alonsolevels.api.events.LevelChangeEvent;
import com.alonsoaliaga.alonsolevels.others.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.google.inject.Inject;

public class LevelChangeListener implements Listener {

    private final Lobby plugin;

    @Inject
    public LevelChangeListener(Lobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChange(LevelChangeEvent event) {
        Player player = event.getPlayer();
        player.setLevel(event.getNewLevel());

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateExperienceBar(player), 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExperienceChange(ExperienceChangeEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateExperienceBar(player), 5L);
    }

    private void updateExperienceBar(Player player) {
        try {
            if (!AlonsoLevelsAPI.isLoaded(player.getUniqueId()))
                return;

            PlayerData playerData = AlonsoLevels.getInstance().getDataMap().get(player.getUniqueId());
            if (playerData == null)
                return;

            int progressPercentage = AlonsoLevels.getInstance().getProgressPercentage(playerData);
            if (progressPercentage != -1) {
                float expProgress = (float) progressPercentage / 100.0f;
                player.setExp(Math.max(0.0f, Math.min(1.0f, expProgress)));
            }
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Error updating experience bar for " + player.getName() + ": " + e.getMessage());
        }
    }
}


package center.bedwars.lobby.nametag;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.util.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.profile.IProfile;
import xyz.refinedev.phoenix.rank.IRank;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager extends Manager {

    private final Map<UUID, PlayerNametag> nametags = new ConcurrentHashMap<>();
    @Getter
    private final NametagFormatter formatter = new NametagFormatter();
    private BukkitTask updateTask;

    @Override
    protected void onLoad() {
        startUpdateTask();
    }

    @Override
    protected void onUnload() {
        stopUpdateTask();
        removeAllNametags();
    }

    public void reload() {
        stopUpdateTask();
        startUpdateTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            removeNametag(player);
            createNametag(player);
        }
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Lobby.getINSTANCE(),
                this::updateAllNametags,
                20L,
                NametagConfiguration.UPDATE_INTERVAL
        );
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void removeAllNametags() {
        nametags.values().forEach(PlayerNametag::remove);
        nametags.clear();
    }

    private void updateAllNametags() {
        Bukkit.getOnlinePlayers().forEach(this::updateNametag);
    }

    public void createNametag(Player player) {
        nametags.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerNametag nametag = new PlayerNametag(player);
            nametag.create();
            updateNametag(player);
            return nametag;
        });
    }

    public void removeNametag(Player player) {
        PlayerNametag nametag = nametags.remove(player.getUniqueId());
        if (nametag != null) {
            nametag.remove();
        }
    }

    private void updateNametag(Player player) {
        PlayerNametag nametag = nametags.get(player.getUniqueId());
        if (nametag == null) return;

        NametagData data = formatNametag(player);
        nametag.update(data);
    }

    private NametagData formatNametag(Player player) {
        String displayName = player.getName();
        String rankName = "Default";
        int priority = 0;

        PhoenixDependency phoenixDependency = getPhoenixDependency();
        if (phoenixDependency != null && phoenixDependency.isApiAvailable()) {
            Phoenix api = phoenixDependency.getApi();
            IProfile profile = api.getProfileHandler().getProfile(player.getUniqueId());
            IRank realRank = api.getGrantHandler().getHighestRank(player.getUniqueId());

            boolean isDisguised = profile != null && profile.getDisguiseData().isDisguised();
            boolean shouldUseDisguise = !shouldShowRealIdentity(realRank);

            if (isDisguised && shouldUseDisguise) {
                IRank disguiseRank = api.getRankHandler().getRank(profile.getDisguiseData().getRankId());
                displayName = profile.getDisguiseData().getDisguiseName();
                rankName = disguiseRank != null ? disguiseRank.getName() : "Default";
                priority = disguiseRank != null ? disguiseRank.getPriority() : 0;
            } else if (realRank != null) {
                rankName = realRank.getName();
                priority = realRank.getPriority();
            }
        }

        NametagConfiguration.GroupConfig config = formatter.getConfig(player, rankName);

        String tagPrefix = formatter.parsePlaceholders(player, config.tagprefix);
        String tagSuffix = formatter.parsePlaceholders(player, config.tagsuffix);

        return new NametagData(
                ColorUtil.color(tagPrefix),
                ColorUtil.color(tagSuffix),
                displayName,
                priority
        );
    }

    private boolean shouldShowRealIdentity(IRank rank) {
        return rank != null && SettingsConfiguration.FULL_DISGUISE_RANKS.contains(rank.getName());
    }

    private PhoenixDependency getPhoenixDependency() {
        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        return dependencyManager != null ? dependencyManager.getPhoenix() : null;
    }

    /**
     * Gets the rank name for a player (for use by TablistManager)
     */
    public String getPlayerRankName(Player player) {
        PhoenixDependency phoenixDependency = getPhoenixDependency();
        if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) {
            return "Default";
        }

        Phoenix api = phoenixDependency.getApi();
        IProfile profile = api.getProfileHandler().getProfile(player.getUniqueId());
        IRank realRank = api.getGrantHandler().getHighestRank(player.getUniqueId());

        boolean isDisguised = profile != null && profile.getDisguiseData().isDisguised();
        boolean shouldUseDisguise = !shouldShowRealIdentity(realRank);

        if (isDisguised && shouldUseDisguise) {
            IRank disguiseRank = api.getRankHandler().getRank(profile.getDisguiseData().getRankId());
            return disguiseRank != null ? disguiseRank.getName() : "Default";
        }

        return realRank != null ? realRank.getName() : "Default";
    }

    /**
     * Gets the display name for a player (considering disguise)
     */
    public String getPlayerDisplayName(Player player) {
        PhoenixDependency phoenixDependency = getPhoenixDependency();
        if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) {
            return player.getName();
        }

        Phoenix api = phoenixDependency.getApi();
        IProfile profile = api.getProfileHandler().getProfile(player.getUniqueId());
        IRank realRank = api.getGrantHandler().getHighestRank(player.getUniqueId());

        boolean isDisguised = profile != null && profile.getDisguiseData().isDisguised();
        boolean shouldUseDisguise = !shouldShowRealIdentity(realRank);

        if (isDisguised && shouldUseDisguise) {
            return profile.getDisguiseData().getDisguiseName();
        }

        return player.getName();
    }
}
package center.bedwars.lobby.nametag;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
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
        String rankName = "Default";
        int priority = 0;

        PhoenixDependency phoenixDependency = getPhoenixDependency();
        if (phoenixDependency != null && phoenixDependency.isApiAvailable()) {
            Phoenix api = phoenixDependency.getApi();
            UUID realUUID = getRealUUID(player, api);
            IRank rank = api.getGrantHandler().getHighestRank(realUUID);

            if (rank != null) {
                rankName = rank.getName();
                priority = rank.getPriority();
            }
        }

        NametagConfiguration.GroupConfig config = formatter.getConfig(player, rankName);

        String tagPrefix = formatter.parsePlaceholders(player, config.tagprefix);
        String tagSuffix = formatter.parsePlaceholders(player, config.tagsuffix);

        return new NametagData(
                ColorUtil.color(tagPrefix),
                ColorUtil.color(tagSuffix),
                player.getName(),
                priority
        );
    }

    private UUID getRealUUID(Player player, Phoenix api) {
        try {
            IProfile profile = api.getProfileHandler().getProfile(player.getUniqueId());
            if (profile != null && profile.getDisguiseData().isDisguised()) {
                return Bukkit.getPlayer(profile.getDisguiseData().getRealName()).getUniqueId();
            }
        } catch (Exception ignored) {}

        return player.getUniqueId();
    }

    private PhoenixDependency getPhoenixDependency() {
        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        return dependencyManager != null ? dependencyManager.getPhoenix() : null;
    }
}
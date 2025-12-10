package center.bedwars.lobby.nametag;

import center.bedwars.api.nametag.Nametag;
import center.bedwars.api.nametag.NametagData;
import center.bedwars.api.util.ColorUtil;
import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.service.AbstractService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.profile.IProfile;
import xyz.refinedev.phoenix.rank.IRank;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class NametagService extends AbstractService implements INametagService {

    private final Lobby plugin;
    private final IDependencyService dependencyService;
    private final Map<UUID, Nametag> nametags = new ConcurrentHashMap<>();
    @Getter
    private final NametagFormatter formatter;
    private BukkitTask updateTask;

    @Inject
    public NametagService(Lobby plugin, IDependencyService dependencyService, NametagFormatter formatter) {
        this.plugin = plugin;
        this.dependencyService = dependencyService;
        this.formatter = formatter;
    }

    @Override
    protected void onEnable() {
        startUpdateTask();
    }

    @Override
    protected void onDisable() {
        stopUpdateTask();
        removeAllNametags();
    }

    @Override
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
                plugin,
                this::updateAllNametags,
                20L,
                NametagConfiguration.UPDATE_INTERVAL);
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void removeAllNametags() {
        nametags.values().forEach(Nametag::remove);
        nametags.clear();
    }

    private void updateAllNametags() {
        Bukkit.getOnlinePlayers().forEach(this::updateNametag);
    }

    @Override
    public void createNametag(Player player) {
        nametags.computeIfAbsent(player.getUniqueId(), uuid -> {
            Nametag nametag = new Nametag(player);
            nametag.create();
            updateNametag(player);
            return nametag;
        });
    }

    @Override
    public void removeNametag(Player player) {
        Nametag nametag = nametags.remove(player.getUniqueId());
        if (nametag != null) {
            nametag.remove();
        }
    }

    @Override
    public void updateNametag(Player player) {
        Nametag nametag = nametags.get(player.getUniqueId());
        if (nametag == null)
            return;

        NametagData data = formatNametag(player);
        nametag.update(data);
    }

    private NametagData formatNametag(Player player) {
        String rankName = "Default";
        String rankPrefix = "";
        int priority = 0;

        PhoenixDependency phoenixDependency = dependencyService.getPhoenix();
        if (phoenixDependency != null && phoenixDependency.isApiAvailable()) {
            Phoenix api = phoenixDependency.getApi();
            UUID realUUID = getRealUUID(player, api);
            IRank rank = api.getGrantHandler().getHighestRank(realUUID);

            if (rank != null) {
                rankName = rank.getName();
                priority = rank.getPriority();
                rankPrefix = rank.getPrefix() != null ? rank.getPrefix() : "";
            }
        }

        NametagConfiguration.GroupConfig config = formatter.getConfig(player, rankName);

        String tagPrefix = config.tagprefix;
        String tagSuffix = config.tagsuffix;

        tagPrefix = tagPrefix.replace("%phoenix_player_rank_prefix%", rankPrefix);
        tagSuffix = tagSuffix.replace("%phoenix_player_rank_prefix%", rankPrefix);

        tagPrefix = formatter.parsePlaceholders(player, tagPrefix);
        tagSuffix = formatter.parsePlaceholders(player, tagSuffix);

        return new NametagData(
                ColorUtil.color(tagPrefix),
                ColorUtil.color(tagSuffix),
                player.getName(),
                priority);
    }

    private UUID getRealUUID(Player player, Phoenix api) {
        try {
            IProfile profile = api.getProfileHandler().getProfile(player.getUniqueId());
            if (profile != null && profile.getDisguiseData().isDisguised()) {
                Player realPlayer = Bukkit.getPlayer(profile.getDisguiseData().getRealName());
                if (realPlayer != null) {
                    return realPlayer.getUniqueId();
                }
            }
        } catch (Exception ignored) {
        }

        return player.getUniqueId();
    }
}

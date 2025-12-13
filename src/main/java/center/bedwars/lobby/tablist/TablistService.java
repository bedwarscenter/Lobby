package center.bedwars.lobby.tablist;

import center.bedwars.api.nms.NMSHelper;
import center.bedwars.api.tablist.Tablist;
import center.bedwars.api.util.ColorUtil;
import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
import center.bedwars.lobby.configuration.configurations.TablistConfiguration;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.nametag.NametagFormatter;
import center.bedwars.lobby.nametag.NametagService;
import center.bedwars.lobby.service.AbstractService;
import me.clip.placeholderapi.PlaceholderAPI;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.profile.IProfile;
import xyz.refinedev.phoenix.rank.IRank;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class TablistService extends AbstractService implements ITablistService {

    private final Lobby plugin;
    private final IDependencyService dependencyService;
    private final Provider<INametagService> nametagServiceProvider;
    private final Map<UUID, Tablist> tablists = new ConcurrentHashMap<>();

    private BukkitTask updateTask;

    @Inject
    public TablistService(Lobby plugin, IDependencyService dependencyService,
            Provider<INametagService> nametagServiceProvider) {
        this.plugin = plugin;
        this.dependencyService = dependencyService;
        this.nametagServiceProvider = nametagServiceProvider;
    }

    @Override
    protected void onEnable() {

        startUpdateTask();
    }

    @Override
    protected void onDisable() {
        stopUpdateTask();
        tablists.clear();
    }

    @Override
    public void reload() {
        stopUpdateTask();

        startUpdateTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            removeTablist(player);
            createTablist(player);
        }
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::updateAllTablists,
                20L,
                TablistConfiguration.UPDATE_INTERVAL);
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateAllTablists() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTablist(player);
        }
    }

    @Override
    public void createTablist(Player player) {
        tablists.computeIfAbsent(player.getUniqueId(), uuid -> {
            Tablist tablist = new Tablist(player);
            updateTablist(player);
            return tablist;
        });
    }

    @Override
    public void removeTablist(Player player) {
        tablists.remove(player.getUniqueId());
    }

    @Override
    public void updateTablist(Player player) {
        Tablist tablist = tablists.get(player.getUniqueId());
        if (tablist == null)
            return;

        String header = formatHeader(player);
        String footer = formatFooter(player);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                tablist.update(header, footer);
            }
        });

        updatePlayerNames(player);
    }

    private String formatHeader(Player player) {
        return TablistConfiguration.HEADER.stream()
                .map(line -> parsePlaceholders(player, line))
                .map(ColorUtil::color)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private String formatFooter(Player player) {
        return TablistConfiguration.FOOTER.stream()
                .map(line -> parsePlaceholders(player, line))
                .map(ColorUtil::color)
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private void updatePlayerNames(Player viewer) {
        INametagService nametagService = nametagServiceProvider.get();
        if (!(nametagService instanceof NametagService))
            return;

        NametagFormatter formatter = ((NametagService) nametagService).getFormatter();
        List<PlayerEntry> entries = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID realUUID = getRealUUID(player);
            IRank rank = getPlayerRank(realUUID);
            String rankName = (rank != null && rank.getName() != null && !rank.getName().isEmpty())
                    ? rank.getName()
                    : "Default";

            NametagConfiguration.GroupConfig config = formatter.getConfig(player, rankName);

            String rankPrefix = (rank != null && rank.getPrefix() != null) ? rank.getPrefix() : "";

            String tabPrefix = config.tabprefix.replace("%phoenix_player_rank_prefix%", rankPrefix);
            String tabSuffix = config.tabsuffix.replace("%phoenix_player_rank_prefix%", rankPrefix);

            tabPrefix = formatter.parsePlaceholders(player, tabPrefix);
            tabSuffix = formatter.parsePlaceholders(player, tabSuffix);

            String formattedName = ColorUtil.color(tabPrefix + player.getName() + tabSuffix);

            int priority = (rank != null && rank.getName() != null && !rank.getName().isEmpty())
                    ? rank.getPriority()
                    : -1;

            entries.add(new PlayerEntry(player, formattedName, priority, rank,
                    ColorUtil.color(tabPrefix), ColorUtil.color(tabSuffix)));
        }

        entries.sort((a, b) -> {
            int priorityCompare = Integer.compare(b.priority(), a.priority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            String nameA = a.player().getName();
            String nameB = b.player().getName();
            if (!TablistConfiguration.CASE_SENSITIVE_SORTING) {
                nameA = nameA.toLowerCase();
                nameB = nameB.toLowerCase();
            }
            return nameA.compareTo(nameB);
        });

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (viewer.isOnline()) {
                int index = 0;
                for (PlayerEntry entry : entries) {
                    if (entry.player().isOnline()) {
                        updatePlayerDisplayName(viewer, entry);
                        setPlayerTeamOrder(viewer, entry.player(), index, entry.prefix(), entry.suffix());
                        index++;
                    }
                }
            }
        });
    }

    private void setPlayerTeamOrder(Player viewer, Player target, int order, String prefix, String suffix) {
        try {
            String teamName = "sb" + String.format("%03d", order);

            PacketPlayOutScoreboardTeam removePacket = new PacketPlayOutScoreboardTeam();
            setTeamField(removePacket, "a", teamName);
            setTeamField(removePacket, "h", 1);
            NMSHelper.sendPacket(viewer, removePacket);

            PacketPlayOutScoreboardTeam createPacket = new PacketPlayOutScoreboardTeam();
            setTeamField(createPacket, "a", teamName);
            setTeamField(createPacket, "h", 0);
            setTeamField(createPacket, "b", "");
            setTeamField(createPacket, "c", truncateTeamText(prefix, 16));
            setTeamField(createPacket, "d", truncateTeamText(suffix, 16));
            setTeamField(createPacket, "i", 0);
            setTeamField(createPacket, "e", "always");
            setTeamField(createPacket, "f", -1);

            Collection<String> members = new ArrayList<>();
            members.add(target.getName());
            setTeamField(createPacket, "g", members);

            NMSHelper.sendPacket(viewer, createPacket);
        } catch (Exception e) {
        }
    }

    private String truncateTeamText(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private void setTeamField(Object packet, String fieldName, Object value) {
        try {
            Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(packet, value);
        } catch (Exception ignored) {
        }
    }

    private IRank getPlayerRank(UUID realUUID) {
        PhoenixDependency phoenixDependency = dependencyService.getPhoenix();
        if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) {
            return null;
        }

        Phoenix api = phoenixDependency.getApi();
        return api.getGrantHandler().getHighestRank(realUUID);
    }

    private UUID getRealUUID(Player player) {
        PhoenixDependency phoenixDependency = dependencyService.getPhoenix();
        if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) {
            return player.getUniqueId();
        }

        try {
            Phoenix api = phoenixDependency.getApi();
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

    @SuppressWarnings("unchecked")
    private void updatePlayerDisplayName(Player viewer, PlayerEntry entry) {
        try {
            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME,
                    NMSHelper.getHandle(entry.player()));

            Field field = packet.getClass().getDeclaredField("b");
            field.setAccessible(true);

            List<PacketPlayOutPlayerInfo.PlayerInfoData> dataList = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) field
                    .get(packet);

            if (dataList != null && !dataList.isEmpty()) {
                Field displayNameField = dataList.get(0).getClass().getDeclaredField("e");
                displayNameField.setAccessible(true);

                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(
                        "{\"text\":\"" + escapeJson(entry.displayName()) + "\"}");
                displayNameField.set(dataList.get(0), component);
            }

            NMSHelper.sendPacket(viewer, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null)
            return "";

        text = text.replace("%player_name%", player.getName())
                .replace("%player%", player.getName());

        PhoenixDependency phoenixDependency = dependencyService.getPhoenix();
        if (phoenixDependency != null && phoenixDependency.isApiAvailable()) {
            try {
                Phoenix api = phoenixDependency.getApi();
                IProfile profile = api.getProfileHandler().getProfile(player.getUniqueId());
                if (profile != null && profile.getDisguiseData().isDisguised()) {
                    text = text.replace(player.getName(), profile.getDisguiseData().getDisguiseName());
                }
            } catch (Exception ignored) {
            }
        }

        PlaceholderAPIDependency placeholderAPI = dependencyService.getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isApiAvailable()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}

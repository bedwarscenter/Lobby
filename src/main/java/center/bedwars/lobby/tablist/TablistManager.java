package center.bedwars.lobby.tablist;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
import center.bedwars.lobby.configuration.configurations.TablistConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.nametag.NametagFormatter;
import center.bedwars.lobby.nametag.NametagManager;
import center.bedwars.lobby.nms.NMSHelper;
import center.bedwars.lobby.tablist.sorting.SortingManager;
import center.bedwars.lobby.util.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.profile.IProfile;
import xyz.refinedev.phoenix.rank.IRank;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TablistManager extends Manager {

    private final Map<UUID, PlayerTablist> tablists = new ConcurrentHashMap<>();
    private final SortingManager sortingManager = new SortingManager();
    private BukkitTask updateTask;

    @Override
    protected void onLoad() {
        sortingManager.reload();
        startUpdateTask();
    }

    @Override
    protected void onUnload() {
        stopUpdateTask();
        clearAllTablists();
    }

    public void reload() {
        stopUpdateTask();
        sortingManager.reload();
        startUpdateTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            removeTablist(player);
            createTablist(player);
        }
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Lobby.getINSTANCE(),
                this::updateAllTablists,
                20L,
                TablistConfiguration.UPDATE_INTERVAL
        );
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void clearAllTablists() {
        tablists.clear();
    }

    private void updateAllTablists() {
        Bukkit.getOnlinePlayers().forEach(this::updateTablist);
    }

    public void createTablist(Player player) {
        tablists.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerTablist tablist = new PlayerTablist(player);
            updateTablist(player);
            return tablist;
        });
    }

    public void removeTablist(Player player) {
        tablists.remove(player.getUniqueId());
    }

    private void updateTablist(Player player) {
        PlayerTablist tablist = tablists.get(player.getUniqueId());
        if (tablist == null) return;

        String header = formatHeader(player);
        String footer = formatFooter(player);

        tablist.update(header, footer);
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
        NametagManager nametagManager = Lobby.getManagerStorage().getManager(NametagManager.class);
        if (nametagManager == null) return;

        NametagFormatter formatter = nametagManager.getFormatter();
        List<PlayerEntry> entries = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID realUUID = getRealUUID(player);
            String rankName = getPlayerRankName(realUUID);

            NametagConfiguration.GroupConfig config = formatter.getConfig(player, rankName);

            String tabPrefix = formatter.parsePlaceholders(player, config.tabprefix);
            String tabSuffix = formatter.parsePlaceholders(player, config.tabsuffix);
            String formattedName = ColorUtil.color(tabPrefix) + player.getName() + ColorUtil.color(tabSuffix);

            entries.add(new PlayerEntry(player, formattedName, 0, null));
        }

        entries.sort((a, b) -> {
            UUID realUUIDA = getRealUUID(a.player());
            UUID realUUIDB = getRealUUID(b.player());
            String rankA = getPlayerRankName(realUUIDA);
            String rankB = getPlayerRankName(realUUIDB);
            return sortingManager.compare(a.player(), b.player(), rankA, rankB);
        });

        for (PlayerEntry entry : entries) {
            updatePlayerDisplayName(viewer, entry);
        }
    }

    private String getPlayerRankName(UUID realUUID) {
        PhoenixDependency phoenixDependency = getPhoenixDependency();
        if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) {
            return "Default";
        }

        Phoenix api = phoenixDependency.getApi();
        IRank rank = api.getGrantHandler().getHighestRank(realUUID);

        return rank != null ? rank.getName() : "Default";
    }

    private UUID getRealUUID(Player player) {
        PhoenixDependency phoenixDependency = getPhoenixDependency();
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
        } catch (Exception ignored) {}

        return player.getUniqueId();
    }

    private PhoenixDependency getPhoenixDependency() {
        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        return dependencyManager != null ? dependencyManager.getPhoenix() : null;
    }

    private void updatePlayerDisplayName(Player viewer, PlayerEntry entry) {
        try {
            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME,
                    NMSHelper.getHandle(entry.player())
            );

            Field field = packet.getClass().getDeclaredField("b");
            field.setAccessible(true);

            List<PacketPlayOutPlayerInfo.PlayerInfoData> dataList =
                    (List<PacketPlayOutPlayerInfo.PlayerInfoData>) field.get(packet);

            if (dataList != null && !dataList.isEmpty()) {
                Field displayNameField = dataList.getFirst().getClass().getDeclaredField("e");
                displayNameField.setAccessible(true);

                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(
                        "{\"text\":\"" + entry.displayName().replace("\"", "\\\"") + "\"}"
                );
                displayNameField.set(dataList.getFirst(), component);
            }

            NMSHelper.sendPacket(viewer, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";

        text = text.replace("%player_name%", player.getName())
                .replace("%player%", player.getName());

        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        if (dependencyManager == null) return text;

        PlaceholderAPIDependency placeholderAPI = dependencyManager.getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isApiAvailable()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}
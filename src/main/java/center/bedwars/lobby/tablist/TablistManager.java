package center.bedwars.lobby.tablist;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.TablistConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.nms.NMSHelper;
import center.bedwars.lobby.util.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.phoenix.rank.IRank;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TablistManager extends Manager {

    private final Map<UUID, PlayerTablist> tablists = new ConcurrentHashMap<>();
    private final TablistFormatter formatter = new TablistFormatter();
    private final PlayerNameUpdater nameUpdater = new PlayerNameUpdater();
    private BukkitTask updateTask;

    @Override
    protected void onLoad() {
        startUpdateTask();
    }

    @Override
    protected void onUnload() {
        stopUpdateTask();
        clearAllTablists();
    }

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

        String header = formatter.formatHeader(player);
        String footer = formatter.formatFooter(player);

        tablist.update(header, footer);
        nameUpdater.updatePlayerNames(player);
    }

    private class TablistFormatter {

        public String formatHeader(Player player) {
            return formatLines(TablistConfiguration.HEADER, player);
        }

        public String formatFooter(Player player) {
            return formatLines(TablistConfiguration.FOOTER, player);
        }

        private String formatLines(List<String> lines, Player player) {
            return lines.stream()
                    .map(line -> parsePlaceholders(player, line))
                    .map(ColorUtil::color)
                    .collect(Collectors.joining("\n"))
                    .trim();
        }
    }

    private class PlayerNameUpdater {

        public void updatePlayerNames(Player viewer) {
            PhoenixDependency phoenixDependency = getPhoenixDependency();
            if (phoenixDependency == null || !phoenixDependency.isApiAvailable()) return;

            List<PlayerEntry> entries = createPlayerEntries(phoenixDependency);
            entries.sort(new PlayerEntryComparator());

            entries.forEach(entry -> updatePlayerDisplayName(viewer, entry));
        }

        private PhoenixDependency getPhoenixDependency() {
            DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
            return dependencyManager != null ? dependencyManager.getPhoenix() : null;
        }

        private List<PlayerEntry> createPlayerEntries(PhoenixDependency phoenixDependency) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(player -> createPlayerEntry(player, phoenixDependency))
                    .collect(Collectors.toList());
        }

        private PlayerEntry createPlayerEntry(Player player, PhoenixDependency phoenixDependency) {
            IRank rank = phoenixDependency.getApi().getGrantHandler().getHighestRank(player.getUniqueId());
            String displayName = new DisplayNameBuilder(player, rank).build();
            int priority = new RankPriorityCalculator(rank).getPriority();

            return new PlayerEntry(player, displayName, priority, rank);
        }

        private void updatePlayerDisplayName(Player viewer, PlayerEntry entry) {
            try {
                PacketPlayOutPlayerInfo packet = createPacket(entry.getPlayer());
                modifyPacketDisplayName(packet, entry.getDisplayName());
                NMSHelper.sendPacket(viewer, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private PacketPlayOutPlayerInfo createPacket(Player target) {
            return new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME,
                    NMSHelper.getHandle(target)
            );
        }

        private void modifyPacketDisplayName(PacketPlayOutPlayerInfo packet, String displayName) throws Exception {
            Field field = packet.getClass().getDeclaredField("b");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<PacketPlayOutPlayerInfo.PlayerInfoData> dataList =
                    (List<PacketPlayOutPlayerInfo.PlayerInfoData>) field.get(packet);

            if (dataList != null && !dataList.isEmpty()) {
                setDisplayName(dataList.get(0), displayName);
            }
        }

        private void setDisplayName(PacketPlayOutPlayerInfo.PlayerInfoData data, String displayName) throws Exception {
            Field displayNameField = data.getClass().getDeclaredField("e");
            displayNameField.setAccessible(true);

            IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(
                    "{\"text\":\"" + displayName.replace("\"", "\\\"") + "\"}"
            );
            displayNameField.set(data, component);
        }
    }

    private class DisplayNameBuilder {
        private final Player player;
        private final IRank rank;

        public DisplayNameBuilder(Player player, IRank rank) {
            this.player = player;
            this.rank = rank;
        }

        public String build() {
            StringBuilder name = new StringBuilder();
            appendRankPrefix(name);
            appendPlayerName(name);
            return parsePlaceholders(player, name.toString());
        }

        private void appendRankPrefix(StringBuilder name) {
            if (rank != null && hasValidPrefix()) {
                name.append(ColorUtil.color(rank.getPrefix())).append(" ");
            }
        }

        private boolean hasValidPrefix() {
            String prefix = rank.getPrefix();
            return prefix != null && !prefix.isEmpty();
        }

        private void appendPlayerName(StringBuilder name) {
            name.append(player.getName());
        }
    }

    private class RankPriorityCalculator {
        private final IRank rank;

        public RankPriorityCalculator(IRank rank) {
            this.rank = rank;
        }

        public int getPriority() {
            if (rank == null) return -1;

            int index = TablistConfiguration.RANK_PRIORITY.indexOf(rank.getName());
            return index >= 0 ? TablistConfiguration.RANK_PRIORITY.size() - index : -1;
        }
    }

    private class PlayerEntryComparator implements Comparator<PlayerEntry> {

        @Override
        public int compare(PlayerEntry a, PlayerEntry b) {
            int priorityComparison = comparePriority(a, b);
            if (priorityComparison != 0) return priorityComparison;

            int mvpPlusComparison = compareMvpPlusColors(a, b);
            if (mvpPlusComparison != 0) return mvpPlusComparison;

            return compareNames(a, b);
        }

        private int comparePriority(PlayerEntry a, PlayerEntry b) {
            return Integer.compare(b.getPriority(), a.getPriority());
        }

        private int compareMvpPlusColors(PlayerEntry a, PlayerEntry b) {
            if (!areBothMvpPlus(a, b)) return 0;

            int aColorPriority = getPlusColorPriority(a.getRank());
            int bColorPriority = getPlusColorPriority(b.getRank());

            return Integer.compare(bColorPriority, aColorPriority);
        }

        private boolean areBothMvpPlus(PlayerEntry a, PlayerEntry b) {
            return a.getRank() != null && b.getRank() != null &&
                    "MVP+".equals(a.getRank().getName()) &&
                    "MVP+".equals(b.getRank().getName());
        }

        private int getPlusColorPriority(IRank rank) {
            if (rank == null || rank.getColorLegacy() == null) return -1;

            int index = TablistConfiguration.PLUS_COLOR_PRIORITY.indexOf(rank.getColorLegacy());
            return index >= 0 ? TablistConfiguration.PLUS_COLOR_PRIORITY.size() - index : -1;
        }

        private int compareNames(PlayerEntry a, PlayerEntry b) {
            return a.getPlayer().getName().compareToIgnoreCase(b.getPlayer().getName());
        }
    }

    private String parsePlaceholders(Player player, String text) {
        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        if (dependencyManager == null) return text;

        PlaceholderAPIDependency placeholderAPI = dependencyManager.getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isApiAvailable()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}
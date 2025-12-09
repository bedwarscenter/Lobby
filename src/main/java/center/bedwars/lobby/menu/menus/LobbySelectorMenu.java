package center.bedwars.lobby.menu.menus;

import center.bedwars.lobby.configuration.configurations.MenuConfiguration;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.dependency.dependencies.PhoenixDependency;
import center.bedwars.lobby.util.ItemBuilder;
import center.bedwars.lobby.util.ServerTransferUtil;
import net.j4c0b3y.api.menu.Menu;
import net.j4c0b3y.api.menu.MenuSize;
import net.j4c0b3y.api.menu.annotation.AutoUpdate;
import net.j4c0b3y.api.menu.button.impl.SimpleButton;
import net.j4c0b3y.api.menu.layer.impl.BackgroundLayer;
import net.j4c0b3y.api.menu.layer.impl.ForegroundLayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.handler.INetworkHandler;
import xyz.refinedev.phoenix.server.IServerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoUpdate(20)
public class LobbySelectorMenu extends Menu {

    private final IDependencyService dependencyService;
    private final PhoenixDependency phoenixDependency;
    private final Phoenix phoenixApi;
    private final List<IServerData> sortedLobbies;
    private final ServerTransferUtil transferUtil;

    public LobbySelectorMenu(Player player, IDependencyService dependencyService, ServerTransferUtil transferUtil) {
        super(color(MenuConfiguration.LOBBY_SELECTOR.TITLE), calculateMenuSize(dependencyService), player);
        this.dependencyService = dependencyService;
        this.phoenixDependency = dependencyService.getPhoenix();
        this.phoenixApi = phoenixDependency.isApiAvailable() ? phoenixDependency.getApi() : null;
        this.sortedLobbies = fetchAndSortLobbies();
        this.transferUtil = transferUtil;
    }

    private static MenuSize calculateMenuSize(IDependencyService dependencyService) {
        PhoenixDependency phoenix = dependencyService.getPhoenix();
        if (!phoenix.isApiAvailable()) {
            return MenuSize.of(1);
        }

        Phoenix api = phoenix.getApi();
        INetworkHandler networkHandler = api.getNetworkHandler();
        String lobbyGroup = MenuConfiguration.LOBBY_SELECTOR.LOBBY_GROUP;

        Set<IServerData> lobbies = networkHandler.getServersInGroup(lobbyGroup);

        if (lobbies.isEmpty()) {
            Set<IServerData> allServers = networkHandler.getServers();
            lobbies = new HashSet<>();
            for (IServerData server : allServers) {
                String name = server.getServerName();
                if (name != null && name.toLowerCase().startsWith("bwl")) {
                    lobbies.add(server);
                }
            }
        }

        int count = lobbies.size();
        if (count <= 9)
            return MenuSize.ONE;
        if (count <= 18)
            return MenuSize.TWO;
        if (count <= 27)
            return MenuSize.THREE;
        if (count <= 36)
            return MenuSize.FOUR;
        if (count <= 45)
            return MenuSize.FIVE;
        return MenuSize.SIX;
    }

    private List<IServerData> fetchAndSortLobbies() {
        if (phoenixApi == null) {
            return new ArrayList<>();
        }

        INetworkHandler networkHandler = phoenixApi.getNetworkHandler();
        String lobbyGroup = MenuConfiguration.LOBBY_SELECTOR.LOBBY_GROUP;

        Set<IServerData> lobbies = networkHandler.getServersInGroup(lobbyGroup);

        if (lobbies.isEmpty()) {
            Set<IServerData> allServers = networkHandler.getServers();
            lobbies = new HashSet<>();
            for (IServerData server : allServers) {
                String name = server.getServerName();
                if (name != null && name.toLowerCase().startsWith("bwl")) {
                    lobbies.add(server);
                }
            }
        }

        List<IServerData> sorted = new ArrayList<>(lobbies);
        sorted.sort(Comparator.comparing(server -> {
            String name = server.getServerName();
            String numPart = name.replaceAll("[^0-9]", "");
            return numPart.isEmpty() ? 0 : Integer.parseInt(numPart);
        }));

        return sorted;
    }

    @Override
    public void setup(BackgroundLayer background, ForegroundLayer foreground) {
        int menuSlots = getTotalSlots();

        ItemStack backgroundPane = new ItemBuilder(
                Material.valueOf(MenuConfiguration.LOBBY_SELECTOR.BACKGROUND.MATERIAL),
                1,
                (short) MenuConfiguration.LOBBY_SELECTOR.BACKGROUND.DATA).name(" ").build();

        for (int i = 0; i < menuSlots; i++) {
            background.set(i, new SimpleButton(backgroundPane));
        }

        if (phoenixApi == null) {
            ItemStack errorItem = new ItemBuilder(Material.valueOf(MenuConfiguration.LOBBY_SELECTOR.ERROR.MATERIAL))
                    .name(color(MenuConfiguration.LOBBY_SELECTOR.ERROR.DISPLAY_NAME))
                    .lore(color(MenuConfiguration.LOBBY_SELECTOR.ERROR.LORE))
                    .build();
            foreground.set(4, new SimpleButton(errorItem));
            return;
        }

        if (sortedLobbies.isEmpty()) {
            ItemStack noLobbiesItem = new ItemBuilder(
                    Material.valueOf(MenuConfiguration.LOBBY_SELECTOR.NO_LOBBIES.MATERIAL))
                    .name(color(MenuConfiguration.LOBBY_SELECTOR.NO_LOBBIES.DISPLAY_NAME))
                    .lore(color(MenuConfiguration.LOBBY_SELECTOR.NO_LOBBIES.LORE))
                    .build();
            foreground.set(4, new SimpleButton(noLobbiesItem));
            return;
        }

        for (int i = 0; i < sortedLobbies.size() && i < menuSlots; i++) {
            IServerData server = sortedLobbies.get(i);
            int lobbyNumber = i + 1;

            ItemStack lobbyItem = createLobbyItem(server, lobbyNumber);

            foreground.set(i, new SimpleButton(lobbyItem, (click) -> {
                Player clicker = (Player) click.getMenu().getPlayer();
                handleLobbyClick(clicker, server);
            }));
        }
    }

    private ItemStack createLobbyItem(IServerData server, int lobbyNumber) {
        int online = server.getOnlinePlayers().size();
        int max = server.getMaxPlayers();
        boolean isOnline = server.isOnline();
        boolean isWhitelisted = server.isWhitelisted();
        boolean isFull = online >= max;

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (!isOnline) {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.STATUS.OFFLINE));
        } else if (isWhitelisted) {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.STATUS.WHITELISTED));
        } else if (isFull) {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.STATUS.FULL));
        } else {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.STATUS.ONLINE));
        }

        String countColor = isFull ? "&c" : (online > max * 0.8 ? "&6" : "&a");
        String playersLine = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.PLAYERS_FORMAT
                .replace("{color}", countColor)
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
        lore.add(color(playersLine));

        lore.add("");

        if (!isOnline) {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.CLICK_LORE.OFFLINE));
        } else if (isWhitelisted) {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.CLICK_LORE.WHITELISTED));
        } else if (isFull) {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.CLICK_LORE.FULL));
        } else {
            lore.add(color(MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.CLICK_LORE.ONLINE));
        }

        String material;
        int data;

        if (!isOnline) {
            material = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.OFFLINE;
            data = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.OFFLINE_DATA;
        } else if (isWhitelisted) {
            material = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.WHITELISTED;
            data = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.WHITELISTED_DATA;
        } else if (isFull) {
            material = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.FULL;
            data = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.FULL_DATA;
        } else {
            material = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.ONLINE;
            data = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.MATERIALS.ONLINE_DATA;
        }

        String displayName = MenuConfiguration.LOBBY_SELECTOR.LOBBY_ITEM.DISPLAY_NAME
                .replace("{number}", String.valueOf(lobbyNumber));

        return new ItemBuilder(Material.valueOf(material), 1, (short) data)
                .name(color(displayName))
                .lore(lore)
                .build();
    }

    private void handleLobbyClick(Player player, IServerData server) {
        if (!server.isOnline()) {
            player.sendMessage(color(MenuConfiguration.LOBBY_SELECTOR.MESSAGES.LOBBY_OFFLINE));
            return;
        }

        if (server.isWhitelisted()) {
            player.sendMessage(color(MenuConfiguration.LOBBY_SELECTOR.MESSAGES.LOBBY_WHITELISTED));
            return;
        }

        if (server.getOnlinePlayers().size() >= server.getMaxPlayers()) {
            player.sendMessage(color(MenuConfiguration.LOBBY_SELECTOR.MESSAGES.LOBBY_FULL));
            return;
        }

        player.closeInventory();

        String transferMessage = MenuConfiguration.LOBBY_SELECTOR.MESSAGES.TRANSFERRING
                .replace("{server}", server.getServerName());
        player.sendMessage(color(transferMessage));

        sendToServer(player, server.getServerName());
    }

    private void sendToServer(Player player, String serverName) {
        transferUtil.sendToServer(player, serverName);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public void onClose() {
    }
}

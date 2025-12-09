package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;

public class MenuConfiguration extends StaticConfig {
    public MenuConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "menus.yml"), handler);
    }

    public static class LOBBY_SELECTOR {
        @Comment("Menu title")
        public static String TITLE = "Lobby Selector";

        @Comment("Server group to fetch lobbies from")
        public static String LOBBY_GROUP = "Lobbies";

        public static class BACKGROUND {
            public static String MATERIAL = "STAINED_GLASS_PANE";
            public static int DATA = 15;
        }

        public static class ERROR {
            public static String MATERIAL = "BARRIER";
            public static String DISPLAY_NAME = "&cError!";
            public static String LORE = "&7API is not available.";
        }

        public static class NO_LOBBIES {
            public static String MATERIAL = "BARRIER";
            public static String DISPLAY_NAME = "&cNo Lobbies Found";
            public static String LORE = "&7There are no active lobbies.";
        }

        public static class LOBBY_ITEM {
            public static String DISPLAY_NAME = "&aLobby {number}";

            public static class MATERIALS {
                public static String ONLINE = "STAINED_CLAY";
                public static int ONLINE_DATA = 5;

                public static String OFFLINE = "STAINED_CLAY";
                public static int OFFLINE_DATA = 14;

                public static String WHITELISTED = "STAINED_CLAY";
                public static int WHITELISTED_DATA = 4;

                public static String FULL = "STAINED_CLAY";
                public static int FULL_DATA = 14;
            }

            public static class STATUS {
                public static String ONLINE = "&7Status: &aOnline";
                public static String OFFLINE = "&7Status: &cOffline";
                public static String WHITELISTED = "&7Status: &6Whitelisted";
                public static String FULL = "&7Status: &cFull";
            }

            public static String PLAYERS_FORMAT = "&7Players: {color}{online}&7/&f{max}";

            public static class CLICK_LORE {
                public static String ONLINE = "&e▶ Click to join!";
                public static String OFFLINE = "&cThis lobby is currently offline!";
                public static String WHITELISTED = "&6This lobby is in whitelist mode!";
                public static String FULL = "&cThis lobby is full!";
            }
        }

        public static class MESSAGES {
            public static String TRANSFERRING = "&aTransferring to {server}...";
            public static String LOBBY_OFFLINE = "&cThis lobby is currently offline!";
            public static String LOBBY_WHITELISTED = "&6This lobby is in whitelist mode!";
            public static String LOBBY_FULL = "&cThis lobby is full!";
        }
    }
}

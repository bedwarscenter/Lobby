package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ItemsConfiguration extends StaticConfig {
    public ItemsConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "items.yml"), handler);
    }

    public static class LOBBY_HOTBAR {
        public static class QUICK_PLAY {
            @Comment("Slot in hotbar")
            public static int SLOT = 0;

            @Comment("Material type")
            public static String MATERIAL = "COMPASS";

            @Comment("Item data/durability")
            public static int DATA = 0;

            @Comment("Display name")
            public static String DISPLAY_NAME = "&aQuick Play &7(Right Click)";

            @Comment("Lore lines")
            public static List<String> LORE = Arrays.asList(
                    "&7Right click to quickly join a game!",
                    "",
                    "&eClick to open!"
            );
        }

        public static class PROFILE {
            public static int SLOT = 1;
            public static String MATERIAL = "SKULL_ITEM";
            public static int DATA = 3;
            public static String DISPLAY_NAME = "&aYour Profile &7(Right Click)";
            public static List<String> LORE = Arrays.asList(
                    "&7View your stats, settings and more!",
                    "",
                    "&eClick to open!"
            );

            @Comment("Use player's skull")
            public static boolean USE_PLAYER_SKULL = true;
        }

        public static class BEDWARS_MENU {
            public static int SLOT = 2;
            public static String MATERIAL = "BED";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&aBedWars Menu &7(Right Click)";
            public static List<String> LORE = Arrays.asList(
                    "&7Select your game mode!",
                    "",
                    "&eClick to open!"
            );
        }

        public static class SHOP {
            public static int SLOT = 4;
            public static String MATERIAL = "EMERALD";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&aShop &7(Right Click)";
            public static List<String> LORE = Arrays.asList(
                    "&7Purchase collectibles and more!",
                    "",
                    "&eClick to open!"
            );
        }

        public static class COLLECTIBLES {
            public static int SLOT = 6;
            public static String MATERIAL = "CHEST";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&aCollectibles &7(Right Click)";
            public static List<String> LORE = Arrays.asList(
                    "&7Manage your collectibles!",
                    "",
                    "&eClick to open!"
            );
        }

        public static class PLAYER_VISIBILITY {
            public static int SLOT = 7;

            public static class VISIBLE {
                public static String MATERIAL = "INK_SACK";
                public static int DATA = 10;
                public static String DISPLAY_NAME = "&aPlayer Visibility &7(Right Click)";
                public static List<String> LORE = Arrays.asList(
                        "&7Toggle player visibility",
                        "",
                        "&aCurrently: &fVisible",
                        "",
                        "&eClick to toggle!"
                );
            }

            public static class HIDDEN {
                public static String MATERIAL = "INK_SACK";
                public static int DATA = 8;
                public static String DISPLAY_NAME = "&cPlayer Visibility &7(Right Click)";
                public static List<String> LORE = Arrays.asList(
                        "&7Toggle player visibility",
                        "",
                        "&cCurrently: &fHidden",
                        "",
                        "&eClick to toggle!"
                );
            }
        }

        public static class LOBBY_SELECTOR {
            public static int SLOT = 8;
            public static String MATERIAL = "NETHER_STAR";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&aLobby Selector &7(Right Click)";
            public static List<String> LORE = Arrays.asList(
                    "&7Change your current lobby!",
                    "",
                    "&eClick to open!"
            );
        }
    }

    public static class PARKOUR_HOTBAR {
        public static class RESET {
            public static int SLOT = 3;
            public static String MATERIAL = "BED";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&c&lReset Parkour";
            public static List<String> LORE = Arrays.asList(
                    "&7Restart from the beginning",
                    "",
                    "&eClick to reset!"
            );
        }

        public static class CHECKPOINT {
            public static int SLOT = 4;
            public static String MATERIAL = "EMERALD";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&a&lCheckpoint";
            public static List<String> LORE = Arrays.asList(
                    "&7Teleport to last checkpoint",
                    "",
                    "&eClick to teleport!"
            );
        }

        public static class EXIT {
            public static int SLOT = 5;
            public static String MATERIAL = "IRON_DOOR";
            public static int DATA = 0;
            public static String DISPLAY_NAME = "&c&lExit Parkour";
            public static List<String> LORE = Arrays.asList(
                    "&7Leave the parkour",
                    "",
                    "&eClick to exit!"
            );
        }
    }
}

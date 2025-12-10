package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;

public class LanguageConfiguration extends StaticConfig {
    public LanguageConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "language.yml"), handler);
    }

    public static class PARKOUR {
        @Comment("Parkour started message")
        public static String STARTED_TITLE = "&6&lPARKOUR STARTED";

        @Comment("Parkour checkpoints info")
        public static String CHECKPOINTS_INFO = "&7Checkpoints: &e%checkpoints%";

        @Comment("Checkpoint reached message")
        public static String CHECKPOINT_REACHED = "&aCheckpoint reached!";

        @Comment("Parkour completed message")
        public static String COMPLETED_TITLE = "&a&lPARKOUR COMPLETED!";

        @Comment("Parkour time message")
        public static String TIME_MESSAGE = "&eTime: &f%time%";

        @Comment("Parkour reset message")
        public static String RESET_MESSAGE = "&cParkour reset!";

        @Comment("Parkour quit message")
        public static String QUIT_MESSAGE = "&cParkour quit!";

        @Comment("Not in parkour message")
        public static String NOT_IN_PARKOUR = "&cYou are not in a parkour!";

        @Comment("Need all checkpoints message")
        public static String NEED_ALL_CHECKPOINTS = "&cYou need to reach all checkpoints to finish!";

        @Comment("Flight disabled message")
        public static String FLIGHT_DISABLED = "&cYou cannot fly during parkour!";

        @Comment("Gamemode change message")
        public static String GAMEMODE_CHANGE = "&cParkour ended due to gamemode change!";

        @Comment("Actionbar format")
        public static String ACTIONBAR_FORMAT = "&7Time: &e%time% &8| &7Checkpoints: &e%current%&7/&e%total%";
    }

    public static class HOLOGRAM {
        @Comment("Start hologram title")
        public static String START_TITLE = "&6&lSTART";

        @Comment("Start hologram subtitle")
        public static String START_SUBTITLE = "&7Step on the plate to begin";

        @Comment("Checkpoint hologram title")
        public static String CHECKPOINT_TITLE = "&e&lCHECKPOINT";

        @Comment("Checkpoint hologram subtitle")
        public static String CHECKPOINT_SUBTITLE = "&aThis is a checkpoint!";

        @Comment("Finish hologram title")
        public static String FINISH_TITLE = "&a&lFINISH";

        @Comment("Finish hologram subtitle")
        public static String FINISH_SUBTITLE = "&7Complete the parkour!";
    }

    public static class HOTBAR {
        public static String QUICK_PLAY = "&aOpening Quick Play...";
        public static String PROFILE = "&aOpening Profile...";
        public static String BEDWARS_MENU = "&aOpening BedWars Menu...";
        public static String SHOP = "&aOpening Shop...";
        public static String COLLECTIBLES = "&aOpening Collectibles...";
        public static String LOBBY_SELECTOR = "&aOpening Lobby Selector...";
        public static String VISIBILITY_HIDDEN = "&cPlayers are now hidden!";
        public static String VISIBILITY_VISIBLE = "&aPlayers are now visible!";
        public static String VISIBILITY_COOLDOWN = "&cPlease wait %seconds%s before toggling visibility again.";
    }

    public static class SPAWN {
        @Comment("Message sent when teleporting to spawn via command")
        public static String TELEPORTED = "&aTeleported to spawn.";

        @Comment("Message sent when spawn is not configured")
        public static String NOT_CONFIGURED = "&cSpawn is not configured.";
    }

    public static class COMMAND {
        public static class PARKOUR_COMMAND {
            @Comment("Parkour command help title")
            public static String TITLE = "&6&lPARKOUR COMMANDS";

            @Comment("Checkpoint command help")
            public static String CHECKPOINT_HELP = "&e/parkour checkpoint &7- Teleport to last checkpoint";

            @Comment("Reset command help")
            public static String RESET_HELP = "&e/parkour reset &7- Reset and quit the parkour";

            @Comment("Quit command help")
            public static String QUIT_HELP = "&e/parkour quit &7- Quit the parkour";
        }

        public static class SYNC_COMMAND {
            @Comment("Sync command help title")
            public static String TITLE = "&6&lLobby Sync Commands";

            @Comment("Config command help")
            public static String CONFIG_HELP = "&e/sync config &7- Push config to all lobbies";

            @Comment("Chunk command help")
            public static String CHUNK_HELP = "&e/sync chunk &7- Sync current chunk";

            @Comment("Area command help")
            public static String AREA_HELP = "&e/sync area <radius> &7- Sync chunk area";

            @Comment("Full command help")
            public static String FULL_HELP = "&e/sync full &7- Full lobby synchronization";

            @Comment("Config push message")
            public static String CONFIG_PUSHING = "&aPushing configuration to all lobbies...";

            @Comment("Config pushed message")
            public static String CONFIG_PUSHED = "&aConfiguration pushed! (Reload time: %time%ms)";

            @Comment("Chunk sync message")
            public static String CHUNK_SYNCING = "&aSynchronizing current chunk...";

            @Comment("Chunk synced message")
            public static String CHUNK_SYNCED = "&aChunk [%x%, %z%] synchronized! (Size: %size% KB)";

            @Comment("Chunk sync failed message")
            public static String CHUNK_FAILED = "&cFailed to synchronize chunk: %error%";

            @Comment("Area sync message")
            public static String AREA_SYNCING = "&aSynchronizing area with radius %radius% chunks...";

            @Comment("Area synced message")
            public static String AREA_SYNCED = "&aArea synchronized! %chunks% chunks (Total: %size% KB)";

            @Comment("Full sync message")
            public static String FULL_SYNCING = "&aInitiating full lobby synchronization...";

            @Comment("Full sync sent message")
            public static String FULL_SENT = "&aFull synchronization request sent!";

            @Comment("Radius error message")
            public static String RADIUS_ERROR = "&cRadius must be between 1 and 20!";
        }

        public static class ADMIN_COMMAND {
            @Comment("Admin command help title")
            public static String TITLE = "&6&lBedWarsLobby &7- Admin Commands";

            @Comment("Reload command help")
            public static String RELOAD_HELP = "&e/bwl reload &7- Reload configurations";

            @Comment("Parkour command help")
            public static String PARKOUR_HELP = "&e/bwl parkour &7- Reload parkours";

            @Comment("Reload message")
            public static String RELOADING = "&aReloading configurations...";

            @Comment("Reloaded message")
            public static String RELOADED = "&aConfigurations reloaded! &7(%time%ms)";

            @Comment("Parkour scanning message")
            public static String PARKOUR_SCANNING = "&aScanning for parkours...";

            @Comment("Parkour refreshed message")
            public static String PARKOUR_REFRESHED = "&aParkours refreshed!";

            @Comment("Parkour failed message")
            public static String PARKOUR_FAILED = "&cFailed to refresh parkours: %error%";
        }
    }
}

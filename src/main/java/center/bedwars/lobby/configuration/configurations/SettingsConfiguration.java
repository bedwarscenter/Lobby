package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsConfiguration extends StaticConfig {
    public SettingsConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "settings.yml"), handler);
    }

    @Comment("Lobby id")
    public static String LOBBY_ID = "bwL1";

    public static class REDIS {
        @Comment("Redis Host - Redis server address (default: localhost)")
        public static String REDIS_HOST = "localhost";

        @Comment("Redis Port - Redis server port (default: 6379)")
        public static int REDIS_PORT = 6379;

        @Comment("Redis Password - Password for Redis authentication (leave empty if not required)")
        public static String REDIS_PASSWORD = "";

        @Comment("Redis Database - Redis database index to use (default: 0)")
        public static int REDIS_DATABASE = 0;

        @Comment("Redis SSL - Enable SSL for Redis connection (default: false)")
        public static boolean REDIS_SSL = false;
    }

    public static class MONGO {
        @Comment("MongoDB Connection URI - Full connection string to MongoDB (e.g., mongodb://localhost:27017/bedwars)")
        public static String MONGO_URI = "mongodb://localhost:27017/bedwars";

        @Comment("MongoDB Database - Target database name (optional if included in URI)")
        public static String MONGO_DATABASE = "bedwars";

        @Comment("MongoDB Connection Timeout - Connection timeout in milliseconds (default: 30000)")
        public static int MONGO_CONNECTION_TIMEOUT = 30000;
    }

    @Comment("Join messages based on rank")
    public static Map<String, String> JOIN_MESSAGES = new HashMap<>() {{
        put("MVP+", "test");
        put("MVP++", "test");
    }};

    @Comment("Spawn location")
    public static String SPAWN_LOCATION = "0.5;60;0.5;world;0;0";

    public static class PLAYER {
        @Comment("Force players into adventure mode on join/respawn")
        public static boolean FORCE_ADVENTURE = true;

        @Comment("Disable hunger changes in the lobby")
        public static boolean DISABLE_HUNGER = true;

        @Comment("Saturation value applied when hunger is disabled")
        public static float SATURATION = 20.0f;

        @Comment("Teleport players to spawn on join")
        public static boolean TELEPORT_ON_JOIN = true;

        @Comment("Teleport players to spawn on respawn")
        public static boolean TELEPORT_ON_RESPAWN = true;

        @Comment("Disable fall damage in the lobby")
        public static boolean DISABLE_FALL_DAMAGE = true;

        @Comment("Automatically teleport players to spawn when falling into the void")
        public static boolean AUTO_TELEPORT_ON_VOID = true;

        @Comment("Y level threshold considered as void for teleportation")
        public static double VOID_Y = 0.0d;

        @Comment("Prevent players from dropping items in the lobby")
        public static boolean DISABLE_ITEM_DROPS = true;

        @Comment("Prevent players from interacting with blocks using lobby items")
        public static boolean BLOCK_INTERACTIONS_WITH_HOTBAR_ITEMS = true;
    }

    public static class PARKOUR_BEHAVIOR {
        public static boolean RESTORE_SAVED_LOCATION = false;
        public static boolean TELEPORT_TO_SPAWN_ON_FINISH = true;
        public static boolean TELEPORT_TO_SPAWN_ON_QUIT = false;
    }

    public static class VISIBILITY {
        @Comment("Mute nearby sounds from hidden players")
        public static boolean MUTE_SOUNDS_WHEN_HIDDEN = true;

        @Comment("Radius to consider a sound close enough for muting")
        public static double SOUND_RADIUS = 4.0d;

        public static List<String> MUTED_SOUNDS = Arrays.asList(
                "step.grass",
                "step.stone",
                "step.wood"
        );

        @Comment("Cooldown in milliseconds between visibility toggles")
        public static long TOGGLE_COOLDOWN_MILLIS = 3000L;
    }

}
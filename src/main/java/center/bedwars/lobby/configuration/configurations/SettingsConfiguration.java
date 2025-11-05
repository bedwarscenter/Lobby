package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;
import xyz.refinedev.phoenix.rank.IRank;

import java.io.File;
import java.util.HashMap;
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

        @Comment("MongoDB Username - Username for MongoDB authentication (fill if required)")
        public static String MONGO_USERNAME = "";

        @Comment("MongoDB Password - Password for MongoDB authentication (fill if required)")
        public static String MONGO_PASSWORD = "";

        @Comment("MongoDB Connection Timeout - Connection timeout in milliseconds (default: 30000)")
        public static int MONGO_CONNECTION_TIMEOUT = 30000;
    }

    public static class JOIN_MESSAGES {
        private static final Map<String, String> messages = new HashMap<>();

        static {
            messages.put("MVP+", "test");
            messages.put("MVP++", "test");
        }

        public static String get(String key) {
            return messages.get(key);
        }

    }

}
package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;

public class SoundConfiguration extends StaticConfig {
    public SoundConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "sounds.yml"), handler);
    }

    public static class PARKOUR {
        @Comment("Parkour start sound")
        public static String START_SOUND = "LEVEL_UP";

        @Comment("Parkour start sound volume")
        public static float START_VOLUME = 1.0f;

        @Comment("Parkour start sound pitch")
        public static float START_PITCH = 1.0f;

        @Comment("Checkpoint reached sound")
        public static String CHECKPOINT_SOUND = "ORB_PICKUP";

        @Comment("Checkpoint sound volume")
        public static float CHECKPOINT_VOLUME = 1.0f;

        @Comment("Checkpoint sound pitch")
        public static float CHECKPOINT_PITCH = 1.5f;

        @Comment("Parkour complete sound")
        public static String COMPLETE_SOUND = "LEVEL_UP";

        @Comment("Parkour complete sound volume")
        public static float COMPLETE_VOLUME = 1.0f;

        @Comment("Parkour complete sound pitch")
        public static float COMPLETE_PITCH = 2.0f;

        @Comment("Parkour reset sound")
        public static String RESET_SOUND = "ENDERMAN_TELEPORT";

        @Comment("Parkour reset sound volume")
        public static float RESET_VOLUME = 1.0f;

        @Comment("Parkour reset sound pitch")
        public static float RESET_PITCH = 1.0f;

        @Comment("Parkour quit sound")
        public static String QUIT_SOUND = "VILLAGER_NO";

        @Comment("Parkour quit sound volume")
        public static float QUIT_VOLUME = 1.0f;

        @Comment("Parkour quit sound pitch")
        public static float QUIT_PITCH = 1.0f;

        @Comment("Checkpoint teleport sound")
        public static String CHECKPOINT_TP_SOUND = "ENDERMAN_TELEPORT";

        @Comment("Checkpoint teleport sound volume")
        public static float CHECKPOINT_TP_VOLUME = 1.0f;

        @Comment("Checkpoint teleport sound pitch")
        public static float CHECKPOINT_TP_PITCH = 1.2f;

        @Comment("Error sound (not in parkour, need all checkpoints, etc)")
        public static String ERROR_SOUND = "VILLAGER_NO";

        @Comment("Error sound volume")
        public static float ERROR_VOLUME = 1.0f;

        @Comment("Error sound pitch")
        public static float ERROR_PITCH = 0.8f;
    }
}

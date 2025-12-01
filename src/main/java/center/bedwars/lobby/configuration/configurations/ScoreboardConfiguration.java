package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ScoreboardConfiguration extends StaticConfig {

    public ScoreboardConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "scoreboard.yml"), handler);
    }

    @Comment("Update interval in ticks (20 ticks = 1 second)")
    public static int UPDATE_INTERVAL = 20;

    @Comment({
            "Animated title frames",
            "Each frame will be displayed for TITLE_ANIMATION_SPEED ticks",
            "Supports color codes and formatting"
    })
    public static List<String> TITLE_FRAMES = Arrays.asList(
            "&f&lBED WARS"
    );

    @Comment("Ticks between title animation frames (smaller = faster)")
    public static int TITLE_ANIMATION_SPEED = 3;

    @Comment({
            "Scoreboard lines",
            "Use empty string for blank lines"
    })
    public static List<String> LINES = Arrays.asList(
            "&7<date> &8<instance>",
            "",
            "&fLevel: &7<level_with_star>",
            "",
            "&fProgress: &b<progress_current>&7/&a<progress_needed>",
            "",
            "&fTokens: &2<tokens>",
            "",
            "&fTotal Kills: &a<total_kills>",
            "&fTotal Wins: &a<total_wins>",
            "",
            "&ewww.bedwars.center"
    );

}

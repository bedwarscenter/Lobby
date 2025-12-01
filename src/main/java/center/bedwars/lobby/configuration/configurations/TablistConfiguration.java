package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TablistConfiguration extends StaticConfig {

    public TablistConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "tablist.yml"), handler);
    }

    @Comment("Update interval in ticks (20 ticks = 1 second)")
    public static int UPDATE_INTERVAL = 20;

    @Comment("Tablist Header")
    public static List<String> HEADER = Arrays.asList(
            "&bYou are playing on &e&lMC.BEDWARS.CENTER"
    );

    @Comment("Tablist Footer")
    public static List<String> FOOTER = Arrays.asList(
            "&aRanks, Boosters & MORE! &c&lSTORE.BEDWARS.CENTER"
    );

    @Comment({
            "Rank priorities for tablist sorting",
            "Higher in the list equals to higher priority in tablist",
            "Format: List order determines priority"
    })
    public static List<String> RANK_PRIORITY = Arrays.asList(
            "Owner",
            ""
    );

    @Comment({
            "Plus color priorities for MVP+ plus colors sorting",
            "Higher in the list equals to higher priority in tablist",
            "Format: Color codes in priority order"
    })
    public static List<String> PLUS_COLOR_PRIORITY = Arrays.asList(
            "&a",
            ""
    );

}
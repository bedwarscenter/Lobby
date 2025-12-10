
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
            "Sorting configuration - works like TAB plugin",
            "Available types:",
            "  GROUPS:group1,group2,group3 - Sort by group order",
            "  PLACEHOLDER_A_TO_Z:%placeholder% - Sort alphabetically A-Z",
            "  PLACEHOLDER_Z_TO_A:%placeholder% - Sort alphabetically Z-A",
            "  PLACEHOLDER_LOW_TO_HIGH:%placeholder% - Sort numerically low to high",
            "  PLACEHOLDER_HIGH_TO_LOW:%placeholder% - Sort numerically high to low",
            "  PLACEHOLDER:%placeholder%:value1,value2,value3 - Sort by placeholder output matching values",
            "",
            "Multiple elements with same priority: use | symbol",
            "Example: GROUPS:owner,admin,vip1|vip2,default",
            "",
            "Sorting types are processed in order - first has highest priority"
    })
    public static List<String> SORTING_TYPES = Arrays.asList(
            "GROUPS:Owner,Admin,Mod,Helper,MVP+,MVP,VIP+,VIP,Default",
            "PLACEHOLDER_A_TO_Z:%player_name%"
    );

    @Comment("Case sensitive sorting for alphabetical sorting")
    public static boolean CASE_SENSITIVE_SORTING = false;
}

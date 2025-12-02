
package center.bedwars.lobby.configuration.configurations;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class NametagConfiguration extends StaticConfig {

    public NametagConfiguration(File file, ConfigHandler handler) {
        super(new File(file, "nametag.yml"), handler);
    }

    @Comment("Update interval in ticks (20 ticks = 1 second)")
    public static int UPDATE_INTERVAL = 20;

    @Comment({
            "Group configurations",
            "Format:",
            "  GroupName:",
            "    tagprefix: '&4&lAdmin &r'",
            "    tagsuffix: ''",
            "    tabprefix: '&4&lAdmin &r'",
            "    tabsuffix: ''",
            "",
            "Use _DEFAULT_ for players without a specific group"
    })
    public static Map<String, GroupConfig> GROUPS = new LinkedHashMap<String, GroupConfig>() {{
        put("Owner", new GroupConfig("&c[Owner] ", "", "&c[Owner] ", ""));
        put("Admin", new GroupConfig("&c[Admin] ", "", "&c[Admin] ", ""));
        put("Mod", new GroupConfig("&9[Mod] ", "", "&9[Mod] ", ""));
        put("Helper", new GroupConfig("&e[Helper] ", "", "&e[Helper] ", ""));
        put("MVP+", new GroupConfig("&6[MVP&c+&6] ", "", "&6[MVP&c+&6] ", ""));
        put("MVP", new GroupConfig("&b[MVP] ", "", "&b[MVP] ", ""));
        put("VIP+", new GroupConfig("&a[VIP&6+&a] ", "", "&a[VIP&6+&a] ", ""));
        put("VIP", new GroupConfig("&a[VIP] ", "", "&a[VIP] ", ""));
        put("_DEFAULT_", new GroupConfig("&7", "", "&7", ""));
    }};

    @Comment({
            "User-specific configurations (overrides group settings)",
            "Can use player name or UUID",
            "Format:",
            "  PlayerName:",
            "    tagprefix: '&6&lTAB &r'",
            "    tagsuffix: ''",
            "    tabprefix: '&6&lTAB &r'",
            "    tabsuffix: ''"
    })
    public static Map<String, GroupConfig> USERS = new LinkedHashMap<>();

    @Ignore
    public static class GroupConfig {
        public String tagprefix;
        public String tagsuffix;
        public String tabprefix;
        public String tabsuffix;

        public GroupConfig() {
            this.tagprefix = "%phoenix_prefix%";
            this.tagsuffix = "";
            this.tabprefix = "%phoenix_prefix%";
            this.tabsuffix = "";
        }

        public GroupConfig(String tagprefix, String tagsuffix, String tabprefix, String tabsuffix) {
            this.tagprefix = tagprefix;
            this.tagsuffix = tagsuffix;
            this.tabprefix = tabprefix;
            this.tabsuffix = tabsuffix;
        }
    }
}
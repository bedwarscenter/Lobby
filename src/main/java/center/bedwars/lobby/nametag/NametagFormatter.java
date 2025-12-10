package center.bedwars.lobby.nametag;

import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
import center.bedwars.lobby.dependency.IDependencyService;
import com.google.inject.Inject;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class NametagFormatter {

    private final IDependencyService dependencyService;

    @Inject
    public NametagFormatter(IDependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    public NametagConfiguration.GroupConfig getConfig(Player player, String rankName) {
        String worldName = player.getWorld().getName();
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();

        NametagConfiguration.GroupConfig userConfig = NametagConfiguration.USERS.get(playerName);
        if (userConfig != null) {
            return userConfig;
        }

        userConfig = NametagConfiguration.USERS.get(playerUUID);
        if (userConfig != null) {
            return userConfig;
        }

        NametagConfiguration.GroupConfig groupConfig = NametagConfiguration.GROUPS.get(rankName);
        if (groupConfig != null) {
            return groupConfig;
        }

        return NametagConfiguration.GROUPS.getOrDefault("_DEFAULT_", new NametagConfiguration.GroupConfig());
    }

    private boolean matchesWorld(String pattern, String worldName) {
        if (pattern.contains(";")) {
            for (String world : pattern.split(";")) {
                if (matchesSingleWorld(world.trim(), worldName)) {
                    return true;
                }
            }
            return false;
        }
        return matchesSingleWorld(pattern, worldName);
    }

    private boolean matchesSingleWorld(String pattern, String worldName) {
        if (pattern.endsWith("*")) {
            return worldName.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return worldName.endsWith(pattern.substring(1));
        }
        return pattern.equals(worldName);
    }

    public String parsePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty())
            return "";

        text = text.replace("%player_name%", player.getName())
                .replace("%player%", player.getName());

        center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency placeholderAPI = dependencyService
                .getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isApiAvailable()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}

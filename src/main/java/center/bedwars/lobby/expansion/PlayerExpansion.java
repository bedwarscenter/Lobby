package center.bedwars.lobby.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "bwlobby_player";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BedwarsCenter";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "name" -> player.getName();
            case "displayname" -> player.getDisplayName();
            case "health" -> String.valueOf(player.getHealth());
            case "level" -> String.valueOf(player.getLevel());
            case "exp" -> String.valueOf(player.getExp());
            case "world" -> player.getWorld().getName();
            default -> null;
        };
    }
}

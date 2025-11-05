package center.bedwars.lobby.parkour.util;

public class ChatColor {

    public static String translate(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
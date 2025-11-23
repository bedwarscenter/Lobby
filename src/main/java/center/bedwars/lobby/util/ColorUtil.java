package center.bedwars.lobby.util;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class ColorUtil {

    private ColorUtil() {}

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(color(text));
    }

    public static void sendActionBar(Player player, String text) {
        IChatBaseComponent comp = IChatBaseComponent.ChatSerializer.a(
                "{\"text\":\"" + color(text) + "\"}");
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                new PacketPlayOutChat(comp, (byte) 2));
    }

    public static void sendTitle(Player player, String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        CraftPlayer cp = (CraftPlayer) player;
        cp.getHandle().playerConnection.sendPacket(
                new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                        IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + color(title) + "\"}"),
                        fadeIn, stay, fadeOut));
        cp.getHandle().playerConnection.sendPacket(
                new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                        IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + color(subtitle) + "\"}"),
                        fadeIn, stay, fadeOut));
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    public static void clearTitle(Player player) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.RESET, null));
    }
}
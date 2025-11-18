package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.util.ColorUtil;
import com.google.gson.JsonObject;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class FullSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();

        if (!data.has("requestingLobby")) {
            return;
        }

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendTitle(player, "&b&lCHANGING INSTANCE", "&7Please wait...", 10, 40, 10);
                player.playSound(player.getLocation(), Sound.PORTAL_TRAVEL, 0.5f, 1.0f);
            }
        });

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);

            for (Player player : Bukkit.getOnlinePlayers()) {
                parkourManager.leaveParkour(player, false);

                World world = Bukkit.getWorld("world");
                if (world != null) {
                    Location spawn = world.getSpawnLocation();
                    Location safeLoc = spawn.clone().add(0, 200, 0);

                    player.teleport(safeLoc);
                }

                sendActionBar(player, "&e&lLoading new instance...");
            }
        }, 20L);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            World world = Bukkit.getWorld("world");
            if (world != null) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    chunk.unload(true, false);
                }
            }

            Lobby.getManagerStorage().getManager(ParkourManager.class).refreshParkours();

            for (Player player : Bukkit.getOnlinePlayers()) {
                sendActionBar(player, "&a&lInstance loaded!");
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                World world = player.getWorld();
                Location spawn = world.getSpawnLocation();

                player.teleport(spawn);
                player.setFallDistance(0);

                sendTitle(player, "&a&lINSTANCE UPDATED", "&7Welcome back!", 10, 30, 10);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }, 60L);
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        IChatBaseComponent titleComponent = IChatBaseComponent.ChatSerializer.a(
                "{\"text\":\"" + ColorUtil.color(title) + "\"}"
        );
        IChatBaseComponent subtitleComponent = IChatBaseComponent.ChatSerializer.a(
                "{\"text\":\"" + ColorUtil.color(subtitle) + "\"}"
        );

        PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(
                PacketPlayOutTitle.EnumTitleAction.TITLE, titleComponent, fadeIn, stay, fadeOut
        );
        PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(
                PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleComponent, fadeIn, stay, fadeOut
        );

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(titlePacket);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitlePacket);
    }

    private void sendActionBar(Player player, String message) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(
                "{\"text\":\"" + ColorUtil.color(message) + "\"}"
        );
        PacketPlayOutChat packet = new PacketPlayOutChat(component, (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}
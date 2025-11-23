package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.util.ColorUtil;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class FullSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                sendTitle(p, "&b&lCHANGING INSTANCE", "&7Please wait...", 10, 40, 10);
                p.playSound(p.getLocation(), Sound.PORTAL_TRAVEL, 0.5f, 1.0f);
            }
        });

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            ParkourManager pm = Lobby.getManagerStorage().getManager(ParkourManager.class);
            for (Player p : Bukkit.getOnlinePlayers()) {
                pm.leaveParkour(p, false);
                Location spawn = p.getWorld().getSpawnLocation().clone().add(0, 200, 0);
                p.teleport(spawn);
                sendActionBar(p, "&e&lLoading new instance...");
            }
        }, 20L);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (org.bukkit.Chunk chunk : Bukkit.getWorld("world").getLoadedChunks()) {
                chunk.unload(true);
            }
            Lobby.getManagerStorage().getManager(ParkourManager.class).refreshParkours();
            for (Player p : Bukkit.getOnlinePlayers()) {
                sendActionBar(p, "&a&lInstance loaded!");
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(p.getWorld().getSpawnLocation());
                p.setFallDistance(0);
                sendTitle(p, "&a&lINSTANCE UPDATED", "&7Welcome back!", 10, 30, 10);
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }, 60L);
    }

    private void sendTitle(Player p, String title, String subtitle, int in, int stay, int out) {
        IChatBaseComponent t = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + ColorUtil.color(title) + "\"}");
        IChatBaseComponent s = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + ColorUtil.color(subtitle) + "\"}");
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(
                new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, t, in, stay, out));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(
                new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, s, in, stay, out));
    }

    private void sendActionBar(Player p, String msg) {
        IChatBaseComponent c = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + ColorUtil.color(msg) + "\"}");
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(c, (byte) 2));
    }
}
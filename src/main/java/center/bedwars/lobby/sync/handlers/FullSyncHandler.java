package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.sync.SyncEvent;
import org.bukkit.Bukkit;

public class FullSyncHandler implements ISyncHandler {

    @Override
    public void handle(SyncEvent event) {
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                sendTitle(p, "&b&lCHANGING INSTANCE", "&7Please wait...", 10, 40, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.PORTAL_TRAVEL, 0.5f, 1.0f);
            }
        });

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            ParkourManager pm = Lobby.getManagerStorage().getManager(ParkourManager.class);
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                pm.leaveParkour(p, false);
                org.bukkit.Location spawn = p.getWorld().getSpawnLocation().clone().add(0, 200, 0);
                p.teleport(spawn);
                sendActionBar(p, "&e&lLoading new instance...");
            }
        }, 20L);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (org.bukkit.Chunk chunk : org.bukkit.Bukkit.getWorld("world").getLoadedChunks()) {
                chunk.unload(true);
            }
            Lobby.getManagerStorage().getManager(ParkourManager.class).refreshParkours();
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                sendActionBar(p, "&a&lInstance loaded!");
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                p.teleport(p.getWorld().getSpawnLocation());
                p.setFallDistance(0);
                sendTitle(p, "&a&lINSTANCE UPDATED", "&7Welcome back!", 10, 30, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }, 60L);
    }

    private void sendTitle(org.bukkit.entity.Player p, String title, String subtitle, int in, int stay, int out) {
        net.minecraft.server.v1_8_R3.IChatBaseComponent t = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + center.bedwars.lobby.util.ColorUtil.color(title) + "\"}");
        net.minecraft.server.v1_8_R3.IChatBaseComponent s = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + center.bedwars.lobby.util.ColorUtil.color(subtitle) + "\"}");
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE, t, in, stay, out));
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.SUBTITLE, s, in, stay, out));
    }

    private void sendActionBar(org.bukkit.entity.Player p, String msg) {
        net.minecraft.server.v1_8_R3.IChatBaseComponent c = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + center.bedwars.lobby.util.ColorUtil.color(msg) + "\"}");
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutChat(c, (byte) 2));
    }
}
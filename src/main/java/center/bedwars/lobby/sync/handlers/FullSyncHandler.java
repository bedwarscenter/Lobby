package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.sync.SyncEvent;
import com.google.inject.Inject;
import org.bukkit.Bukkit;

public class FullSyncHandler implements ISyncHandler {

    private final Lobby plugin;
    private final IParkourService parkourService;

    @Inject
    public FullSyncHandler(Lobby plugin, IParkourService parkourService) {
        this.plugin = plugin;
        this.parkourService = parkourService;
    }

    @Override
    public void handle(SyncEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                sendTitle(p, "&b&lCHANGING INSTANCE", "&7Please wait...", 10, 40, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.PORTAL_TRAVEL, 0.5f, 1.0f);
            }
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                parkourService.leaveParkour(p, false);
                org.bukkit.Location spawn = p.getWorld().getSpawnLocation().clone().add(0, 200, 0);
                p.teleport(spawn);
                sendActionBar(p, "&e&lLoading new instance...");
            }
        }, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.Chunk chunk : Bukkit.getWorld("world").getLoadedChunks()) {
                chunk.unload(true);
            }
            parkourService.refreshParkours();
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                sendActionBar(p, "&a&lInstance loaded!");
            }
        }, 40L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(p.getWorld().getSpawnLocation());
                p.setFallDistance(0);
                sendTitle(p, "&a&lINSTANCE UPDATED", "&7Welcome back!", 10, 30, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }, 60L);
    }

    private void sendTitle(org.bukkit.entity.Player p, String title, String subtitle, int in, int stay, int out) {
        net.minecraft.server.v1_8_R3.IChatBaseComponent t = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer
                .a("{\"text\":\"" + center.bedwars.lobby.util.ColorUtil.color(title) + "\"}");
        net.minecraft.server.v1_8_R3.IChatBaseComponent s = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer
                .a("{\"text\":\"" + center.bedwars.lobby.util.ColorUtil.color(subtitle) + "\"}");
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection
                .sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(
                        net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.TITLE, t, in, stay, out));
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection
                .sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutTitle(
                        net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction.SUBTITLE, s, in, stay, out));
    }

    private void sendActionBar(org.bukkit.entity.Player p, String msg) {
        net.minecraft.server.v1_8_R3.IChatBaseComponent c = net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer
                .a("{\"text\":\"" + center.bedwars.lobby.util.ColorUtil.color(msg) + "\"}");
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection
                .sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutChat(c, (byte) 2));
    }
}
package center.bedwars.lobby.parkour.task;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.nms.NMSHelper;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.ColorUtil;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ParkourActionBarTask extends BukkitRunnable {

    private final ParkourManager parkourManager;

    public ParkourActionBarTask(ParkourManager parkourManager) {
        this.parkourManager = parkourManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ParkourSession session = parkourManager.getSessionManager().getSession(player);
            if (session == null) continue;

            long elapsed = session.getElapsedTime();
            int checkpoints = session.getReachedCheckpoints().size();
            int total = session.getParkour().getCheckpoints().size();

            String time = formatTime(elapsed);
            String message = ColorUtil.color(
                    String.format("&6⏱ &e%s &8| &7Checkpoints: &e%d&7/&e%d", time, checkpoints, total)
            );

            sendActionBar(player, message);
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void sendActionBar(Player player, String message) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + message + "\"}");
        PacketPlayOutChat packet = new PacketPlayOutChat(component, (byte) 2);
        NMSHelper.sendPacket(player, packet);
    }
}
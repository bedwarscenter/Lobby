package center.bedwars.lobby.parkour.task;

import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
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
                    LanguageConfiguration.PARKOUR.ACTIONBAR_FORMAT
                            .replace("%time%", time)
                            .replace("%current%", String.valueOf(checkpoints))
                            .replace("%total%", String.valueOf(total))
            );

            sendActionBar(player, message);
        }
    }

    private String formatTime(long millis) {
        long minutes = millis / (60 * 1000);
        long seconds = (millis % (60 * 1000)) / 1000;
        long milliseconds = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }

    private void sendActionBar(Player player, String message) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + message + "\"}");
        PacketPlayOutChat packet = new PacketPlayOutChat(component, (byte) 2);
        NMSHelper.sendPacket(player, packet);
    }
}
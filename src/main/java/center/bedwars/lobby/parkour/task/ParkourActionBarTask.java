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
            int current = session.getReachedCheckpoints().size();
            int total = session.getParkour().getCheckpoints().size();

            String message = ColorUtil.color(LanguageConfiguration.PARKOUR.ACTIONBAR_FORMAT
                    .replace("%time%", formatTime(elapsed))
                    .replace("%current%", String.valueOf(current))
                    .replace("%total%", String.valueOf(total)));

            sendActionBar(player, message);
        }
    }

    private String formatTime(long millis) {
        return String.format("%02d:%02d.%03d", millis / 60000,
                (millis % 60000) / 1000, millis % 1000);
    }

    private void sendActionBar(Player player, String message) {
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(
                "{\"text\":\"" + message + "\"}");
        NMSHelper.sendPacket(player, new PacketPlayOutChat(component, (byte) 2));
    }
}
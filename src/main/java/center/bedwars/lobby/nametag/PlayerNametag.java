package center.bedwars.lobby.nametag;

import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Collections;

public class PlayerNametag {

    private final Player player;
    private final String teamName;
    private final NametagCache cache;

    public PlayerNametag(Player player) {
        this.player = player;
        this.teamName = "nt_" + player.getUniqueId().toString().substring(0, 8);
        this.cache = new NametagCache();
    }

    public void create() {
        NametagData data = new NametagData("", "", player.getName(), 0);
        update(data);
    }

    public void update(NametagData data) {
        if (cache.hasChanged(data)) {
            removeTeam();
            createTeam(data);
            cache.update(data);
        }
    }

    public void remove() {
        removeTeam();
    }

    private void createTeam(NametagData data) {
        try {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            setField(packet, "a", teamName);
            setField(packet, "b", "");
            setField(packet, "c", truncate(data.prefix(), 16));
            setField(packet, "d", truncate(data.suffix(), 16));
            setField(packet, "e", "always");
            setField(packet, "h", 0);
            setField(packet, "g", Collections.singletonList(player.getName()));

            broadcastPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeTeam() {
        try {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            setField(packet, "a", teamName);
            setField(packet, "h", 1);

            broadcastPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastPacket(PacketPlayOutScoreboardTeam packet) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            NMSHelper.sendPacket(online, packet);
        }
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private void setField(Object object, String fieldName, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class NametagCache {
        private String currentPrefix = "";
        private String currentSuffix = "";
        private String currentName = "";

        public boolean hasChanged(NametagData data) {
            return !currentPrefix.equals(data.prefix()) ||
                    !currentSuffix.equals(data.suffix()) ||
                    !currentName.equals(data.name());
        }

        public void update(NametagData data) {
            this.currentPrefix = data.prefix();
            this.currentSuffix = data.suffix();
            this.currentName = data.name();
        }
    }
}
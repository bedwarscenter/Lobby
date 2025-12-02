package center.bedwars.lobby.tablist;

import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PlayerTablist {

    private final HeaderFooterCache cache;
    private final PacketSender packetSender;

    public PlayerTablist(Player player) {
        this.cache = new HeaderFooterCache();
        this.packetSender = new PacketSender(player);
    }

    public void update(String header, String footer) {
        if (cache.hasChanged(header, footer)) {
            packetSender.sendHeaderFooter(header, footer);
            cache.update(header, footer);
        }
    }

    private static class HeaderFooterCache {
        private String currentHeader = "";
        private String currentFooter = "";

        public boolean hasChanged(String header, String footer) {
            return !currentHeader.equals(header) || !currentFooter.equals(footer);
        }

        public void update(String header, String footer) {
            this.currentHeader = header;
            this.currentFooter = footer;
        }
    }

    private static class PacketSender {
        private final Player player;
        private final PacketBuilder packetBuilder;

        public PacketSender(Player player) {
            this.player = player;
            this.packetBuilder = new PacketBuilder();
        }

        public void sendHeaderFooter(String header, String footer) {
            try {
                PacketPlayOutPlayerListHeaderFooter packet = packetBuilder.build(header, footer);
                NMSHelper.sendPacket(player, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class PacketBuilder {

        public PacketPlayOutPlayerListHeaderFooter build(String header, String footer) throws Exception {
            PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
            setHeaderField(packet, header);
            setFooterField(packet, footer);
            return packet;
        }

        private void setHeaderField(PacketPlayOutPlayerListHeaderFooter packet, String header) throws Exception {
            setField(packet, "a", createComponent(header));
        }

        private void setFooterField(PacketPlayOutPlayerListHeaderFooter packet, String footer) throws Exception {
            setField(packet, "b", createComponent(footer));
        }

        private IChatBaseComponent createComponent(String text) {
            return IChatBaseComponent.ChatSerializer.a(
                    "{\"text\":\"" + escapeJson(text) + "\"}"
            );
        }

        private String escapeJson(String text) {
            return text.replace("\"", "\\\"");
        }

        private void setField(Object object, String fieldName, Object value) throws Exception {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        }
    }
}
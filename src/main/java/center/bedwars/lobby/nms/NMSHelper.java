package center.bedwars.lobby.nms;

import center.bedwars.lobby.nms.netty.NettyService;
import center.bedwars.lobby.nms.netty.PacketDirection;
import io.netty.channel.Channel;
import lombok.experimental.UtilityClass;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@UtilityClass
public class NMSHelper {

    public static EntityPlayer getHandle(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        NettyService.sendPacket(player, packet);
    }

    public static Channel getChannel(Player player) {
        return NettyService.getChannel(player);
    }

    public static int getPing(Player player) {
        return NettyService.getPing(player);
    }

    public static NetworkManager getNetworkManager(Player player) {
        return NettyService.getNetworkManager(player);
    }

    public static <T extends Packet<?>> void listenIncoming(Player player, String listenerName,
            Class<T> packetClass,
            BiConsumer<Player, T> handler) {
        NettyService.listenIncoming(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void listenOutgoing(Player player, String listenerName,
            Class<T> packetClass,
            BiConsumer<Player, T> handler) {
        NettyService.listenOutgoing(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void listenBoth(Player player, String listenerName,
            Class<T> packetClass,
            BiConsumer<Player, T> handler) {
        NettyService.listenBoth(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void cancelIncoming(Player player, String name, Class<T> packetClass) {
        NettyService.cancelIncoming(player, name, packetClass);
    }

    public static <T extends Packet<?>> void cancelOutgoing(Player player, String name, Class<T> packetClass) {
        NettyService.cancelOutgoing(player, name, packetClass);
    }

    public static <T extends Packet<?>> void cancelPacketIf(Player player, String name, Class<T> packetClass,
            BiPredicate<Player, T> condition,
            boolean incoming, boolean outgoing) {
        PacketDirection direction = determineDirection(incoming, outgoing);
        if (direction != null) {
            NettyService.cancelIf(player, name, packetClass, condition, direction);
        }
    }

    public static void removePacketListener(Player player, String listenerName) {
        NettyService.removeHandler(player, listenerName);
    }

    public static void removeAllPacketListeners(Player player) {
        NettyService.removeAllHandlers(player);
    }

    public static boolean hasPacketListener(Player player, String listenerName) {
        return NettyService.hasHandler(player, listenerName);
    }

    public static void sendModifiedPacket(Player player, Packet<?> modifiedPacket) {
        getChannel(player).pipeline().writeAndFlush(modifiedPacket);
    }

    public static void cleanup(Player player) {
        NettyService.cleanup(player);
    }

    public static void cleanupAll() {
        NettyService.cleanupAll();
    }

    public static boolean isConnected(Player player) {
        return NettyService.isConnected(player);
    }

    public static int getActiveHandlers(Player player) {
        return NettyService.getInterceptorIfPresent(player)
                .map(i -> i.getHandlers().size())
                .orElse(0);
    }

    public static int getTotalActiveInterceptors() {
        return NettyService.getActiveInterceptors();
    }

    public static int getTotalActiveHandlers() {
        return NettyService.getTotalHandlers();
    }

    private static PacketDirection determineDirection(boolean incoming, boolean outgoing) {
        if (incoming && outgoing)
            return PacketDirection.BOTH;
        if (incoming)
            return PacketDirection.INCOMING;
        if (outgoing)
            return PacketDirection.OUTGOING;
        return null;
    }
}
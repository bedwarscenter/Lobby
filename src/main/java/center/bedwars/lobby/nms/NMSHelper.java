package center.bedwars.lobby.nms;

import center.bedwars.lobby.nms.netty.NettyManager;
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
        NettyManager.sendPacket(player, packet);
    }

    public static Channel getChannel(Player player) {
        return NettyManager.getChannel(player);
    }

    public static int getPing(Player player) {
        return NettyManager.getPing(player);
    }

    public static NetworkManager getNetworkManager(Player player) {
        return NettyManager.getNetworkManager(player);
    }

    public static <T extends Packet<?>> void listenIncoming(Player player, String listenerName,
                                                            Class<T> packetClass,
                                                            BiConsumer<Player, T> handler) {
        NettyManager.listenIncoming(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void listenOutgoing(Player player, String listenerName,
                                                            Class<T> packetClass,
                                                            BiConsumer<Player, T> handler) {
        NettyManager.listenOutgoing(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void listenBoth(Player player, String listenerName,
                                                        Class<T> packetClass,
                                                        BiConsumer<Player, T> handler) {
        NettyManager.listenBoth(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void cancelIncoming(Player player, String name, Class<T> packetClass) {
        NettyManager.cancelIncoming(player, name, packetClass);
    }

    public static <T extends Packet<?>> void cancelOutgoing(Player player, String name, Class<T> packetClass) {
        NettyManager.cancelOutgoing(player, name, packetClass);
    }

    public static <T extends Packet<?>> void cancelPacketIf(Player player, String name, Class<T> packetClass,
                                                            BiPredicate<Player, T> condition,
                                                            boolean incoming, boolean outgoing) {
        PacketDirection direction = determineDirection(incoming, outgoing);
        if (direction != null) {
            NettyManager.cancelIf(player, name, packetClass, condition, direction);
        }
    }

    public static void removePacketListener(Player player, String listenerName) {
        NettyManager.removeHandler(player, listenerName);
    }

    public static void removeAllPacketListeners(Player player) {
        NettyManager.removeAllHandlers(player);
    }

    public static boolean hasPacketListener(Player player, String listenerName) {
        return NettyManager.hasHandler(player, listenerName);
    }

    public static void sendModifiedPacket(Player player, Packet<?> modifiedPacket) {
        getChannel(player).pipeline().writeAndFlush(modifiedPacket);
    }

    public static void cleanup(Player player) {
        NettyManager.cleanup(player);
    }

    public static void cleanupAll() {
        NettyManager.cleanupAll();
    }

    public static boolean isConnected(Player player) {
        return NettyManager.isConnected(player);
    }

    public static int getActiveHandlers(Player player) {
        return NettyManager.getInterceptorIfPresent(player)
                .map(i -> i.getHandlers().size())
                .orElse(0);
    }

    public static int getTotalActiveInterceptors() {
        return NettyManager.getActiveInterceptors();
    }

    public static int getTotalActiveHandlers() {
        return NettyManager.getTotalHandlers();
    }

    private static PacketDirection determineDirection(boolean incoming, boolean outgoing) {
        if (incoming && outgoing) return PacketDirection.BOTH;
        if (incoming) return PacketDirection.INCOMING;
        if (outgoing) return PacketDirection.OUTGOING;
        return null;
    }
}
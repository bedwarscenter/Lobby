package center.bedwars.lobby.nms;

import center.bedwars.lobby.nms.netty.NettyManager;
import center.bedwars.lobby.nms.netty.PacketDirection;
import io.netty.channel.*;
import lombok.experimental.UtilityClass;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@UtilityClass
@SuppressWarnings({"unused"})
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

    public static <T extends Packet<?>> void listenIncomingPacket(Player player, String listenerName,
                                                                  Class<T> packetClass,
                                                                  BiConsumer<Player, T> handler) {
        NettyManager.listenIncoming(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void listenOutgoingPacket(Player player, String listenerName,
                                                                  Class<T> packetClass,
                                                                  BiConsumer<Player, T> handler) {
        NettyManager.listenOutgoing(player, listenerName, packetClass, handler);
    }

    public static <T extends Packet<?>> void listenBothPackets(Player player, String listenerName,
                                                               Class<T> packetClass,
                                                               BiConsumer<Player, T> handler) {
        NettyManager.listenBoth(player, listenerName, packetClass, handler);
    }

    @Deprecated
    public static void addPacketListener(Player player, String listenerName, BiConsumer<Player, Packet<?>> listener) {
        NettyManager.listenBoth(player, listenerName, (Class<Packet<?>>) (Class<?>) Packet.class, listener);
    }

    public static <T extends Packet<?>> void cancelIncomingPacket(Player player, String name, Class<T> packetClass) {
        NettyManager.cancelIncoming(player, name, packetClass);
    }

    public static <T extends Packet<?>> void cancelOutgoingPacket(Player player, String name, Class<T> packetClass) {
        NettyManager.cancelOutgoing(player, name, packetClass);
    }

    public static <T extends Packet<?>> void cancelPacketIf(Player player, String name, Class<T> packetClass,
                                                            BiPredicate<Player, T> condition,
                                                            boolean incoming, boolean outgoing) {
        PacketDirection direction;
        if (incoming && outgoing) {
            direction = PacketDirection.BOTH;
        } else if (incoming) {
            direction = PacketDirection.INCOMING;
        } else if (outgoing) {
            direction = PacketDirection.OUTGOING;
        } else {
            return;
        }

        NettyManager.cancelIf(player, name, packetClass, condition, direction);
    }

    @Deprecated
    public static void cancelPacket(Player player, String listenerName, Class<? extends Packet<?>> packetClass) {
        NettyManager.cancelIncoming(player, listenerName, packetClass);
        NettyManager.cancelOutgoing(player, listenerName + "_out", packetClass);
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

    @Deprecated
    public static void removeCancelledPacket(Player player, String listenerName) {
        NettyManager.removeHandler(player, listenerName);
        NettyManager.removeHandler(player, listenerName + "_out");
    }

    public static void sendModifiedPacket(Player player, Packet<?> originalPacket, Packet<?> modifiedPacket) {
        Channel channel = getChannel(player);
        channel.pipeline().writeAndFlush(modifiedPacket);
    }

    @Deprecated
    public static void addPacketCounter(Player player, String counterName) {
        final int[] counter = {0};
        addPacketListener(player, counterName, (p, packet) -> counter[0]++);
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
}
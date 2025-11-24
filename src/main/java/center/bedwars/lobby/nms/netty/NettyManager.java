package center.bedwars.lobby.nms.netty;

import io.netty.channel.Channel;
import lombok.experimental.UtilityClass;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.NetworkManager;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@UtilityClass
public class NettyManager {

    private static final Map<UUID, PacketInterceptor> INTERCEPTORS = new ConcurrentHashMap<>();

    public static PacketInterceptor getInterceptor(Player player) {
        return INTERCEPTORS.computeIfAbsent(player.getUniqueId(), uuid ->
                new PacketInterceptor(player, getChannel(player))
        );
    }

    public static Channel getChannel(Player player) {
        return getHandle(player).playerConnection.networkManager.channel;
    }

    public static EntityPlayer getHandle(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        getHandle(player).playerConnection.sendPacket(packet);
    }

    public static <T extends Packet<?>> void listenIncoming(Player player, String name,
                                                            Class<T> packetClass,
                                                            BiConsumer<Player, T> handler) {
        getInterceptor(player).addListener(name, packetClass, handler, PacketDirection.INCOMING);
    }

    public static <T extends Packet<?>> void listenOutgoing(Player player, String name,
                                                            Class<T> packetClass,
                                                            BiConsumer<Player, T> handler) {
        getInterceptor(player).addListener(name, packetClass, handler, PacketDirection.OUTGOING);
    }

    public static <T extends Packet<?>> void listenBoth(Player player, String name,
                                                        Class<T> packetClass,
                                                        BiConsumer<Player, T> handler) {
        getInterceptor(player).addListener(name, packetClass, handler, PacketDirection.BOTH);
    }

    public static <T extends Packet<?>> void cancelIncoming(Player player, String name, Class<T> packetClass) {
        getInterceptor(player).addCanceller(name, packetClass, PacketDirection.INCOMING);
    }

    public static <T extends Packet<?>> void cancelOutgoing(Player player, String name, Class<T> packetClass) {
        getInterceptor(player).addCanceller(name, packetClass, PacketDirection.OUTGOING);
    }

    public static <T extends Packet<?>> void cancelIf(Player player, String name,
                                                      Class<T> packetClass,
                                                      BiPredicate<Player, T> condition,
                                                      PacketDirection direction) {
        getInterceptor(player).addConditionalCanceller(name, packetClass, condition.negate(), direction);
    }

    public static void removeHandler(Player player, String name) {
        PacketInterceptor interceptor = INTERCEPTORS.get(player.getUniqueId());
        if (interceptor != null) {
            interceptor.removeHandler(name);
        }
    }

    public static void removeAllHandlers(Player player) {
        PacketInterceptor interceptor = INTERCEPTORS.remove(player.getUniqueId());
        if (interceptor != null) {
            interceptor.removeAllHandlers();
        }
    }

    public static boolean hasHandler(Player player, String name) {
        PacketInterceptor interceptor = INTERCEPTORS.get(player.getUniqueId());
        return interceptor != null && interceptor.hasHandler(name);
    }

    public static int getPing(Player player) {
        return getHandle(player).ping;
    }

    public static NetworkManager getNetworkManager(Player player) {
        return getHandle(player).playerConnection.networkManager;
    }

    public static boolean isConnected(Player player) {
        PacketInterceptor interceptor = INTERCEPTORS.get(player.getUniqueId());
        return interceptor != null && interceptor.isActive();
    }

    public static Optional<PacketInterceptor> getInterceptorIfPresent(Player player) {
        return Optional.ofNullable(INTERCEPTORS.get(player.getUniqueId()));
    }

    public static void cleanup(Player player) {
        removeAllHandlers(player);
    }

    public static void cleanupAll() {
        INTERCEPTORS.values().forEach(PacketInterceptor::removeAllHandlers);
        INTERCEPTORS.clear();
    }

    public static int getActiveInterceptors() {
        return INTERCEPTORS.size();
    }

    public static int getTotalHandlers() {
        return INTERCEPTORS.values().stream()
                .mapToInt(i -> i.getHandlers().size())
                .sum();
    }

    public static void broadcastSwing(Player p, boolean mainHand){
        PacketPlayOutAnimation pkt = new PacketPlayOutAnimation(
                ((CraftPlayer)p).getHandle(), mainHand ? 0 : 3);
        for(Player viewer : org.bukkit.Bukkit.getOnlinePlayers()){
            if(viewer.equals(p)) continue;
            ((CraftPlayer)viewer).getHandle().playerConnection.sendPacket(pkt);
        }
    }
}
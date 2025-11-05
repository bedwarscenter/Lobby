package center.bedwars.lobby.nms.netty;

import io.netty.channel.Channel;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@Getter
public class PacketInterceptor {

    private final Player player;
    private final Channel channel;
    private final Map<String, CustomNettyHandler> handlers;
    private final Map<String, PacketListenerWrapper<?>> listeners;

    public PacketInterceptor(Player player, Channel channel) {
        this.player = player;
        this.channel = channel;
        this.handlers = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
    }

    public <T extends Packet<?>> void addListener(String name, Class<T> packetClass,
                                                  BiConsumer<Player, T> consumer,
                                                  PacketDirection direction) {
        if (handlers.containsKey(name) || listeners.containsKey(name)) {
            return;
        }

        PacketListenerWrapper<T> wrapper = new PacketListenerWrapper<>(packetClass, consumer);
        listeners.put(name, wrapper);

        BiPredicate<Player, Packet<?>> filter = (p, packet) -> {
            if (packetClass.isInstance(packet)) {
                try {
                    consumer.accept(p, packetClass.cast(packet));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        };

        addHandler(name, filter, direction);
    }

    public <T extends Packet<?>> void addCanceller(String name, Class<T> packetClass,
                                                   PacketDirection direction) {
        if (handlers.containsKey(name)) {
            return;
        }

        BiPredicate<Player, Packet<?>> filter = (p, packet) -> !packetClass.isInstance(packet);
        addHandler(name, filter, direction);
    }

    public <T extends Packet<?>> void addConditionalCanceller(String name, Class<T> packetClass,
                                                              BiPredicate<Player, T> condition,
                                                              PacketDirection direction) {
        if (handlers.containsKey(name)) {
            return;
        }

        BiPredicate<Player, Packet<?>> filter = (p, packet) -> {
            if (packetClass.isInstance(packet)) {
                try {
                    return condition.test(p, packetClass.cast(packet));
                } catch (Exception e) {
                    e.printStackTrace();
                    return true;
                }
            }
            return true;
        };

        addHandler(name, filter, direction);
    }

    private void addHandler(String name, BiPredicate<Player, Packet<?>> filter,
                            PacketDirection direction) {
        if (!channel.isActive()) {
            return;
        }

        String handlerKey = "custom_handler_" + name;

        if (channel.pipeline().get(handlerKey) != null) {
            channel.pipeline().remove(handlerKey);
        }

        CustomNettyHandler handler = new CustomNettyHandler(player, name, filter, direction);
        handlers.put(name, handler);

        try {
            channel.pipeline().addBefore("packet_handler", handlerKey, handler);
        } catch (NoSuchElementException e) {
            channel.pipeline().addLast(handlerKey, handler);
        }
    }

    public void removeHandler(String name) {
        CustomNettyHandler handler = handlers.remove(name);
        listeners.remove(name);

        if (handler != null) {
            handler.deactivate();

            String handlerKey = "custom_handler_" + name;
            if (channel.pipeline().get(handlerKey) != null) {
                channel.pipeline().remove(handlerKey);
            }
        }
    }

    public void removeAllHandlers() {
        List<String> handlerNames = new ArrayList<>(handlers.keySet());

        for (String name : handlerNames) {
            removeHandler(name);
        }

        handlers.clear();
        listeners.clear();
    }

    public boolean hasHandler(String name) {
        return handlers.containsKey(name);
    }

    public Set<String> getHandlerNames() {
        return new HashSet<>(handlers.keySet());
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    private static class PacketListenerWrapper<T extends Packet<?>> {
        private final Class<T> packetClass;
        private final BiConsumer<Player, T> consumer;

        PacketListenerWrapper(Class<T> packetClass, BiConsumer<Player, T> consumer) {
            this.packetClass = packetClass;
            this.consumer = consumer;
        }
    }
}
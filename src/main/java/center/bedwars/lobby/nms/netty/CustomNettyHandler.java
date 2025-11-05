package center.bedwars.lobby.nms.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.Packet;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

@Getter
public class CustomNettyHandler extends ChannelDuplexHandler {

    private final Player player;
    private final String handlerName;
    private final BiPredicate<Player, Packet<?>> packetFilter;
    private final PacketDirection direction;
    private final AtomicBoolean active;

    public CustomNettyHandler(Player player, String handlerName,
                              BiPredicate<Player, Packet<?>> packetFilter,
                              PacketDirection direction) {
        this.player = player;
        this.handlerName = handlerName;
        this.packetFilter = packetFilter;
        this.direction = direction;
        this.active = new AtomicBoolean(true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!active.get() || direction == PacketDirection.OUTGOING) {
            super.channelRead(ctx, msg);
            return;
        }

        if (msg instanceof Packet<?> && !processPacket((Packet<?>) msg)) {
            return;
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!active.get() || direction == PacketDirection.INCOMING) {
            super.write(ctx, msg, promise);
            return;
        }

        if (msg instanceof Packet<?> && !processPacket((Packet<?>) msg)) {
            return;
        }

        super.write(ctx, msg, promise);
    }

    private boolean processPacket(Packet<?> packet) {
        try {
            return packetFilter.test(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    }

    public void deactivate() {
        active.set(false);
    }

    public boolean isActive() {
        return active.get();
    }
}
package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.nms.netty.NettyManager;
import center.bedwars.lobby.sync.EntityPlayerSyncManager;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class EntityPacketListener implements Listener {

    private final EntityPlayerSyncManager syncManager;

    public EntityPacketListener() {
        this.syncManager = Lobby.getManagerStorage().getManager(EntityPlayerSyncManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        syncManager.handlePlayerJoin(e.getPlayer());
        setupIncoming(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        syncManager.handlePlayerQuit(e.getPlayer());
        NettyManager.cleanup(e.getPlayer());
    }

    private void setupIncoming(Player player) {
        NettyManager.listenIncoming(player, "arm_animation", PacketPlayInArmAnimation.class,
                (p, packet) -> {
                    syncManager.handleAnimation(p, 0);
                    NettyManager.broadcastSwing(p, true);
                });

        NettyManager.listenIncoming(player, "entity_action", PacketPlayInEntityAction.class, (p, packet) -> {
            PacketPlayInEntityAction.EnumPlayerAction action = packet.b();
            switch (action) {
                case START_SNEAKING:
                    syncManager.handleSneakChange(p, true);
                    break;
                case STOP_SNEAKING:
                    syncManager.handleSneakChange(p, false);
                    break;
                case START_SPRINTING:
                    syncManager.handleSprintChange(p, true);
                    break;
                case STOP_SPRINTING:
                    syncManager.handleSprintChange(p, false);
                    break;
            }
        });

        NettyManager.listenIncoming(player, "held_item", PacketPlayInHeldItemSlot.class,
                (p, packet) -> syncManager.handleHeldSlotChange(p, packet.a()));

        NettyManager.listenIncoming(player, "use_entity", PacketPlayInUseEntity.class, (p, packet) -> {
            if (packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
                syncManager.handleAnimation(p, 0);
                NettyManager.broadcastSwing(p, true);
            }
        });

        NettyManager.listenIncoming(player, "block_dig", PacketPlayInBlockDig.class, (p, packet) -> {
            PacketPlayInBlockDig.EnumPlayerDigType type = packet.c();
            if (type == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK ||
                    type == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
                syncManager.handleAnimation(p, 0);
                NettyManager.broadcastSwing(p, true);
            }
        });

        NettyManager.listenIncoming(player, "block_place", PacketPlayInBlockPlace.class, (p, packet) -> {
            if (packet.getFace() != -1 && packet.getItemStack() != null) {
                syncManager.handleAnimation(p, 1);
                NettyManager.broadcastSwing(p, true);
            }
        });
    }
}
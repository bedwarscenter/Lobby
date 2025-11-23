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

public class EntityPlayerSyncListener implements Listener {

    private final EntityPlayerSyncManager syncManager;

    public EntityPlayerSyncListener() {
        this.syncManager = Lobby.getManagerStorage().getManager(EntityPlayerSyncManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        syncManager.handlePlayerJoin(player);
        setupPacketListeners(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        syncManager.handlePlayerQuit(player);
        NettyManager.cleanup(player);
    }

    private void setupPacketListeners(Player player) {
        NettyManager.listenIncoming(player, "arm_animation", PacketPlayInArmAnimation.class, (p, packet) -> syncManager.handleAnimation(p, 0));

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

        NettyManager.listenIncoming(player, "held_item", PacketPlayInHeldItemSlot.class, (p, packet) -> syncManager.handleHeldSlotChange(p, packet.a()));

        NettyManager.listenIncoming(player, "use_entity", PacketPlayInUseEntity.class, (p, packet) -> {
            if (packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
                syncManager.handleAnimation(p, 0);
            }
        });

        NettyManager.listenIncoming(player, "block_dig", PacketPlayInBlockDig.class, (p, packet) -> {
            if (packet.c() == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK ||
                    packet.c() == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
                syncManager.handleAnimation(p, 0);
            }
        });

        NettyManager.listenIncoming(player, "block_place", PacketPlayInBlockPlace.class, (p, packet) -> syncManager.handleAnimation(p, 1));
    }
}
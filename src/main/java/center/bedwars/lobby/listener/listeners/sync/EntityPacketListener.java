package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.sync.IPlayerSyncService;
import center.bedwars.lobby.sync.serialization.PlayerSerializer.PlayerSyncAction;
import center.bedwars.api.nms.netty.NettyManager;
import com.google.inject.Inject;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Locale;

public final class EntityPacketListener implements Listener {

    private final IPlayerSyncService playerSyncService;

    @Inject
    public EntityPacketListener(IPlayerSyncService playerSyncService) {
        this.playerSyncService = playerSyncService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        playerSyncService.handlePlayerJoin(e.getPlayer());
        setupIncoming(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        playerSyncService.handlePlayerQuit(e.getPlayer());
        NettyManager.cleanup(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null)
            return;

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ() ||
                from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {

            String posData = String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f,%.2f,%b",
                    to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch(), e.getPlayer().isOnGround());
            playerSyncService.broadcast(e.getPlayer(), PlayerSyncAction.POSITION, posData);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent e) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        playerSyncService.broadcast(e.getPlayer(), PlayerSyncAction.FLY, String.valueOf(e.isFlying()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            playerSyncService.broadcast(player, PlayerSyncAction.DAMAGE, null);
        }
    }

    private void setupIncoming(Player player) {
        NettyManager.listenIncoming(player, "arm_animation", PacketPlayInArmAnimation.class,
                (p, packet) -> {
                    playerSyncService.broadcast(p, PlayerSyncAction.ANIMATION, "0");
                    NettyManager.broadcastSwing(p, true);
                });

        NettyManager.listenIncoming(player, "entity_action", PacketPlayInEntityAction.class, (p, packet) -> {
            PacketPlayInEntityAction.EnumPlayerAction action = packet.b();
            switch (action) {
                case START_SNEAKING:
                    playerSyncService.broadcast(p, PlayerSyncAction.SNEAK, "true");
                    break;
                case STOP_SNEAKING:
                    playerSyncService.broadcast(p, PlayerSyncAction.SNEAK, "false");
                    break;
                case START_SPRINTING:
                    playerSyncService.broadcast(p, PlayerSyncAction.SPRINT, "true");
                    break;
                case STOP_SPRINTING:
                    playerSyncService.broadcast(p, PlayerSyncAction.SPRINT, "false");
                    break;
                case STOP_SLEEPING:
                case OPEN_INVENTORY:
                case RIDING_JUMP:
                    break;
            }
        });

        NettyManager.listenIncoming(player, "held_item", PacketPlayInHeldItemSlot.class,
                (p, packet) -> playerSyncService.broadcast(p, PlayerSyncAction.HELD_ITEM, String.valueOf(packet.a())));

        NettyManager.listenIncoming(player, "use_entity", PacketPlayInUseEntity.class, (p, packet) -> {
            if (packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
                playerSyncService.broadcast(p, PlayerSyncAction.ANIMATION, "0");
                NettyManager.broadcastSwing(p, true);
            }
        });

        NettyManager.listenIncoming(player, "block_dig", PacketPlayInBlockDig.class, (p, packet) -> {
            PacketPlayInBlockDig.EnumPlayerDigType type = packet.c();
            if (type == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK ||
                    type == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
                playerSyncService.broadcast(p, PlayerSyncAction.ANIMATION, "0");
                NettyManager.broadcastSwing(p, true);
            }
        });

        NettyManager.listenIncoming(player, "block_place", PacketPlayInBlockPlace.class, (p, packet) -> {
            if (packet.getFace() != -1 && packet.getItemStack() != null) {
                playerSyncService.broadcast(p, PlayerSyncAction.ANIMATION, "1");
                NettyManager.broadcastSwing(p, true);
            }
        });
    }
}

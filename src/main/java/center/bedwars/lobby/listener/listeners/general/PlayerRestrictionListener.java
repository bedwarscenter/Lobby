package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockPlace;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;

public class PlayerRestrictionListener implements Listener {

    private static final String INTERACTION_HANDLER = "bwl_hotbar_block";

    private final HotbarManager hotbarManager;

    public PlayerRestrictionListener() {
        this.hotbarManager = Lobby.getManagerStorage().getManager(HotbarManager.class);
        Bukkit.getOnlinePlayers().forEach(this::registerPacketInterceptor);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        registerPacketInterceptor(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        NMSHelper.removePacketListener(event.getPlayer(), INTERACTION_HANDLER);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!SettingsConfiguration.PLAYER.DISABLE_ITEM_DROPS) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (!SettingsConfiguration.PLAYER.BLOCK_INTERACTIONS_WITH_HOTBAR_ITEMS) {
            return;
        }

        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item == null || !isLobbyItem(item)) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    private void registerPacketInterceptor(Player player) {
        if (!SettingsConfiguration.PLAYER.BLOCK_INTERACTIONS_WITH_HOTBAR_ITEMS) {
            return;
        }

        NMSHelper.listenIncomingPacket(
                player,
                INTERACTION_HANDLER,
                PacketPlayInBlockPlace.class,
                (p, packet) -> modifyPacketAnimation(packet)
        );
    }

    private void modifyPacketAnimation(PacketPlayInBlockPlace packet) {
        if (!SettingsConfiguration.PLAYER.BLOCK_INTERACTIONS_WITH_HOTBAR_ITEMS) {
            return;
        }

        ItemStack nmsItem = packet.getItemStack();
        if (nmsItem == null) {
            return;
        }

        org.bukkit.inventory.ItemStack bukkitItem = CraftItemStack.asBukkitCopy(nmsItem);
        if (bukkitItem == null) {
            return;
        }

        if (hotbarManager == null) {
            return;
        }

        if (hotbarManager.isParkourItem(bukkitItem)) {
            return;
        }

        if (!isLobbyItem(bukkitItem)) {
            return;
        }

        try {
            Field faceField = PacketPlayInBlockPlace.class.getDeclaredField("face");
            faceField.setAccessible(true);
            faceField.setInt(packet, 255);
        } catch (Exception e) {
            try {
                Field faceField = PacketPlayInBlockPlace.class.getDeclaredField("d");
                faceField.setAccessible(true);
                faceField.setInt(packet, 255);
            } catch (Exception ex) {
            }
        }
    }

    private boolean isLobbyItem(org.bukkit.inventory.ItemStack item) {
        if (hotbarManager == null) {
            return false;
        }
        return hotbarManager.isLobbyItem(item);
    }
}


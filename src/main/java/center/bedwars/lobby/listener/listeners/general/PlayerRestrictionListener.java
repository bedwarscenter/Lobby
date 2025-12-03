package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRestrictionListener implements Listener {

    private static final String INTERACTION_HANDLER = "bwl_hotbar_block";
    private final Map<UUID, Map<Long, Material>> fakeBlocks = new HashMap<>();
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
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!SettingsConfiguration.PLAYER.DISABLE_ITEM_DROPS) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("BuildMode")) {
            return;
        }

        event.setCancelled(true);

        Block block = event.getBlock();
        sendFakeBlockBreak(player, block);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("BuildMode")) {
            return;
        }

        event.setCancelled(true);

        Block block = event.getBlock();
        sendFakeBlockPlace(player, block, event.getBlockPlaced().getType());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.WATER ||
                block.getType() == Material.STATIONARY_WATER ||
                block.getType() == Material.LAVA ||
                block.getType() == Material.STATIONARY_LAVA) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        if (type == Material.CROPS || type == Material.CARROT ||
                type == Material.POTATO || type == Material.WHEAT ||
                type == Material.SOIL) {
            event.setCancelled(true);
        }

        if (type == Material.WATER || type == Material.STATIONARY_WATER ||
                type == Material.LAVA || type == Material.STATIONARY_LAVA) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockGrow(BlockGrowEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockSpread(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("BuildMode")) {
            return;
        }

        if (event.getClickedBlock() != null) {
            if (event.getItem() != null && event.getItem().getType() == Material.INK_SACK &&
                    event.getItem().getDurability() == 15) {
                return;
            }

            Material type = event.getClickedBlock().getType();

            if (isInteractable(type)) {
                event.setCancelled(true);
                return;
            }
        }

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

        NMSHelper.listenIncoming(
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

    private void sendFakeBlockBreak(Player player, Block block) {
        sendFakeBlockChange(player, block, Material.AIR);
        storeFakeBlock(player, block, Material.AIR);
    }

    private void sendFakeBlockPlace(Player player, Block block, Material material) {
        sendFakeBlockChange(player, block, material);
        storeFakeBlock(player, block, material);
    }

    private void sendFakeBlockChange(Player player, Block block, Material material) {
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(
                ((CraftWorld) block.getWorld()).getHandle(),
                new BlockPosition(block.getX(), block.getY(), block.getZ())
        );

        net.minecraft.server.v1_8_R3.Block nmsBlock = net.minecraft.server.v1_8_R3.Block.getById(material.getId());
        IBlockData blockData = nmsBlock.getBlockData();

        try {
            java.lang.reflect.Field blockField = PacketPlayOutBlockChange.class.getDeclaredField("block");
            blockField.setAccessible(true);
            blockField.set(packet, blockData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NMSHelper.sendPacket(player, packet);
    }

    private void storeFakeBlock(Player player, Block block, Material material) {
        UUID uuid = player.getUniqueId();
        fakeBlocks.putIfAbsent(uuid, new HashMap<>());

        long key = getBlockKey(block);
        fakeBlocks.get(uuid).put(key, material);
    }

    private long getBlockKey(Block block) {
        return ((long) block.getX() & 0x3FFFFFF) |
                (((long) block.getZ() & 0x3FFFFFF) << 26) |
                (((long) block.getY() & 0xFFF) << 52);
    }

    private boolean isInteractable(Material type) {
        return switch (type) {
            case WOODEN_DOOR, IRON_DOOR, TRAP_DOOR, FENCE_GATE, CHEST, TRAPPED_CHEST, FURNACE, BURNING_FURNACE,
                 DISPENSER, DROPPER, HOPPER, BREWING_STAND, ENCHANTMENT_TABLE, ANVIL, BEACON, LEVER, STONE_BUTTON,
                 WOOD_BUTTON, WORKBENCH, ENDER_CHEST, DRAGON_EGG, DIODE_BLOCK_OFF, DIODE_BLOCK_ON,
                 REDSTONE_COMPARATOR_OFF, REDSTONE_COMPARATOR_ON -> true;
            default -> false;
        };
    }

    private boolean isLobbyItem(org.bukkit.inventory.ItemStack item) {
        if (hotbarManager == null) {
            return false;
        }
        return hotbarManager.isLobbyItem(item);
    }

    private void cleanupPlayer(Player player) {
        fakeBlocks.remove(player.getUniqueId());
    }
}
package center.bedwars.lobby.listener.listeners.general;

import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldChangeListener implements Listener {

    private final Map<UUID, Map<Long, Material>> fakeBlocks = new HashMap<>();

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("BuildMode")) {
            return;
        }

        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();

        if (isInteractable(type)) {
            event.setCancelled(true);
        }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
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

    public void cleanupPlayer(Player player) {
        fakeBlocks.remove(player.getUniqueId());
    }
}
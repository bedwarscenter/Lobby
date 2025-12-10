package center.bedwars.lobby.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.configuration.configurations.SoundConfiguration;
import center.bedwars.lobby.hotbar.IHotbarService;
import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.model.ParkourCheckpoint;
import center.bedwars.lobby.parkour.session.ParkourSession;

import center.bedwars.lobby.parkour.task.ParkourActionBarTask;
import center.bedwars.lobby.player.PlayerStateSnapshot;
import center.bedwars.lobby.service.AbstractService;
import center.bedwars.api.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Getter
public class ParkourService extends AbstractService implements IParkourService {

    private final Lobby plugin;
    private final Logger logger;
    private final IHotbarService hotbarService;

    private final Map<String, Parkour> parkours = new HashMap<>();
    private final Map<UUID, ParkourSession> sessions = new HashMap<>();
    private final Map<UUID, Integer> playerCompletions = new HashMap<>();

    public ParkourSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    private final Map<UUID, PlayerStateSnapshot> savedStates = new HashMap<>();
    private final Map<String, Location> blockLocationCache = new HashMap<>();
    private BukkitTask actionBarTask;

    @Inject
    public ParkourService(Lobby plugin, Logger logger, IHotbarService hotbarService) {
        this.plugin = plugin;
        this.logger = logger;
        this.hotbarService = hotbarService;
    }

    @Override
    protected void onEnable() {
        this.actionBarTask = new ParkourActionBarTask(this).runTaskTimer(plugin, 0L, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, this::scanAndInitializeParkours, 20L);
    }

    @Override
    protected void onDisable() {
        if (actionBarTask != null)
            actionBarTask.cancel();
        parkours.values().forEach(this::removeHolograms);
        parkours.clear();
        sessions.clear();
        playerCompletions.clear();
        savedStates.clear();
        blockLocationCache.clear();
    }

    @Override
    public void refreshParkours() {
        parkours.values().forEach(this::removeHolograms);
        parkours.clear();
        blockLocationCache.clear();
        scanAndInitializeParkours();
    }

    private void scanAndInitializeParkours() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            logger.warning("World 'world' not found, cannot scan parkours.");
            return;
        }

        Map<String, Block> goldBlocks = new HashMap<>();
        Map<String, Block> ironBlocks = new HashMap<>();
        Map<String, Block> diamondBlocks = new HashMap<>();

        org.bukkit.WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2;

        int minChunkX = ((int) (center.getX() - radius)) >> 4;
        int maxChunkX = ((int) (center.getX() + radius)) >> 4;
        int minChunkZ = ((int) (center.getZ() - radius)) >> 4;
        int maxChunkZ = ((int) (center.getZ() + radius)) >> 4;

        logger.info("Starting parkour scan...");

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            Material type = block.getType();

                            if (type == Material.GOLD_BLOCK || type == Material.IRON_BLOCK
                                    || type == Material.DIAMOND_BLOCK) {
                                Block above = block.getRelative(0, 1, 0);
                                if (above.getType() == Material.WOOD_PLATE) {
                                    String key = locationKey(block.getLocation());

                                    if (type == Material.GOLD_BLOCK) {
                                        goldBlocks.put(key, block);
                                    } else if (type == Material.IRON_BLOCK) {
                                        ironBlocks.put(key, block);
                                    } else {
                                        diamondBlocks.put(key, block);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<String> processed = new HashSet<>();
        logger.info(String.format("Found %d GOLD_BLOCKs, %d IRON_BLOCKs, %d DIAMOND_BLOCKs",
                goldBlocks.size(), ironBlocks.size(), diamondBlocks.size()));

        for (Map.Entry<String, Block> entry : goldBlocks.entrySet()) {
            if (processed.contains(entry.getKey()))
                continue;

            Block startBlock = entry.getValue();
            Parkour parkour = buildParkour(startBlock, ironBlocks, diamondBlocks, processed);

            if (parkour != null) {
                parkours.put(parkour.getId(), parkour);
                cacheBlockLocations(parkour);
                createHolograms(parkour);
            }
        }

        Bukkit.getLogger().info("Found " + parkours.size() + " parkours");
    }

    private Parkour buildParkour(Block startBlock, Map<String, Block> ironBlocks,
            Map<String, Block> diamondBlocks, Set<String> processed) {
        Location start = startBlock.getLocation();
        processed.add(locationKey(start));

        List<ParkourCheckpoint> checkpoints = new ArrayList<>();
        Location finish = null;

        int searchRadius = 250;

        for (Map.Entry<String, Block> entry : ironBlocks.entrySet()) {
            if (processed.contains(entry.getKey()))
                continue;

            Location loc = entry.getValue().getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(start.getWorld()) &&
                    Math.abs(loc.getBlockX() - start.getBlockX()) <= searchRadius &&
                    Math.abs(loc.getBlockY() - start.getBlockY()) <= searchRadius &&
                    Math.abs(loc.getBlockZ() - start.getBlockZ()) <= searchRadius) {

                checkpoints.add(new ParkourCheckpoint(0, loc.clone()));
                processed.add(entry.getKey());
            }
        }

        for (Map.Entry<String, Block> entry : diamondBlocks.entrySet()) {
            if (processed.contains(entry.getKey()))
                continue;

            Location loc = entry.getValue().getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(start.getWorld()) &&
                    Math.abs(loc.getBlockX() - start.getBlockX()) <= searchRadius &&
                    Math.abs(loc.getBlockY() - start.getBlockY()) <= searchRadius &&
                    Math.abs(loc.getBlockZ() - start.getBlockZ()) <= searchRadius) {

                finish = loc.clone();
                processed.add(entry.getKey());
                break;
            }
        }

        if (finish == null)
            return null;

        checkpoints.sort(Comparator.comparingDouble(cp -> cp.location().distanceSquared(start)));
        for (int i = 0; i < checkpoints.size(); i++) {
            checkpoints.set(i, new ParkourCheckpoint(i + 1, checkpoints.get(i).location()));
        }

        return new Parkour("parkour_" + UUID.randomUUID().toString().substring(0, 8),
                start, checkpoints, finish);
    }

    private void cacheBlockLocations(Parkour parkour) {
        blockLocationCache.put(locationKey(parkour.getStartLocation()), parkour.getStartLocation());

        for (ParkourCheckpoint cp : parkour.getCheckpoints()) {
            blockLocationCache.put(locationKey(cp.location()), cp.location());
        }

        blockLocationCache.put(locationKey(parkour.getFinishLocation()), parkour.getFinishLocation());
    }

    private String locationKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void createHolograms(Parkour parkour) {
        try {
            Hologram startHologram = createHologram(parkour.getId() + "_start",
                    parkour.getStartLocation(), LanguageConfiguration.HOLOGRAM.START_TITLE,
                    LanguageConfiguration.HOLOGRAM.START_SUBTITLE);
            parkour.setStartHologram(startHologram);

            for (ParkourCheckpoint cp : parkour.getCheckpoints()) {
                Hologram checkpointHologram = createHologram(parkour.getId() + "_cp_" + cp.number(),
                        cp.location(), LanguageConfiguration.HOLOGRAM.CHECKPOINT_TITLE,
                        LanguageConfiguration.HOLOGRAM.CHECKPOINT_SUBTITLE);
                if (checkpointHologram != null) {
                    parkour.addCheckpointHologram(cp.number(), checkpointHologram);
                }
            }

            Hologram finishHologram = createHologram(parkour.getId() + "_finish",
                    parkour.getFinishLocation(), LanguageConfiguration.HOLOGRAM.FINISH_TITLE,
                    LanguageConfiguration.HOLOGRAM.FINISH_SUBTITLE);
            parkour.setFinishHologram(finishHologram);
        } catch (Exception e) {
            logger.warning("Failed to create holograms for parkour " + parkour.getId());
        }
    }

    private Hologram createHologram(String id, Location loc, String title, String subtitle) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }

        try {
            Location hologramLocation = loc.clone().add(0.5, 2.5, 0.5);
            Hologram h = DHAPI.createHologram(id, hologramLocation);
            DHAPI.setHologramLines(h, Arrays.asList(ColorUtil.color(title), ColorUtil.color(subtitle)));
            return h;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create hologram " + id + ": " + e.getMessage());
            return null;
        }
    }

    private void removeHolograms(Parkour parkour) {
        try {
            if (parkour.getStartHologram() != null)
                DHAPI.removeHologram(parkour.getStartHologram().getName());
            parkour.getCheckpointHolograms().values()
                    .forEach(h -> DHAPI.removeHologram(h.getName()));
            if (parkour.getFinishHologram() != null)
                DHAPI.removeHologram(parkour.getFinishHologram().getName());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void startParkour(Player player, Parkour parkour) {
        ParkourSession existing = getSession(player);

        if (existing != null) {

            sessions.remove(player.getUniqueId());
            sessions.put(player.getUniqueId(), new ParkourSession(player, parkour));
            sendMessage(player, LanguageConfiguration.PARKOUR.STARTED_TITLE,
                    LanguageConfiguration.PARKOUR.CHECKPOINTS_INFO
                            .replace("%checkpoints%", String.valueOf(parkour.getCheckpoints().size())));
            playSound(player, SoundConfiguration.PARKOUR.START_SOUND,
                    SoundConfiguration.PARKOUR.START_VOLUME, SoundConfiguration.PARKOUR.START_PITCH);
            return;
        }

        savedStates.put(player.getUniqueId(), PlayerStateSnapshot.capture(player));
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setAllowFlight(false);
        player.setFlying(false);
        hotbarService.giveParkourHotbar(player);
        sessions.put(player.getUniqueId(), new ParkourSession(player, parkour));
        sendMessage(player, LanguageConfiguration.PARKOUR.STARTED_TITLE,
                LanguageConfiguration.PARKOUR.CHECKPOINTS_INFO
                        .replace("%checkpoints%", String.valueOf(parkour.getCheckpoints().size())));
        playSound(player, SoundConfiguration.PARKOUR.START_SOUND,
                SoundConfiguration.PARKOUR.START_VOLUME, SoundConfiguration.PARKOUR.START_PITCH);
    }

    @Override
    public void handleCheckpoint(Player player, Location location) {
        ParkourSession session = getSession(player);
        if (session == null)
            return;

        ParkourCheckpoint cp = session.getParkour().getCheckpointAt(location);
        if (cp == null || session.hasReachedCheckpoint(cp.number()))
            return;

        session.reachCheckpoint(cp.number());
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.CHECKPOINT_REACHED);
        playSound(player, SoundConfiguration.PARKOUR.CHECKPOINT_SOUND,
                SoundConfiguration.PARKOUR.CHECKPOINT_VOLUME, SoundConfiguration.PARKOUR.CHECKPOINT_PITCH);
    }

    @Override
    public void handleFinish(Player player, Location location) {
        ParkourSession session = getSession(player);
        if (session == null)
            return;

        if (!session.getParkour().getFinishLocation().getBlock().getLocation()
                .equals(location.getBlock().getLocation()))
            return;

        if (session.getReachedCheckpoints().size() != session.getParkour().getCheckpoints().size()) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NEED_ALL_CHECKPOINTS);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME, SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        playerCompletions.merge(player.getUniqueId(), 1, Integer::sum);
        sendMessage(player, LanguageConfiguration.PARKOUR.COMPLETED_TITLE,
                LanguageConfiguration.PARKOUR.TIME_MESSAGE
                        .replace("%time%", formatTime(session.getElapsedTime())));
        playSound(player, SoundConfiguration.PARKOUR.COMPLETE_SOUND,
                SoundConfiguration.PARKOUR.COMPLETE_VOLUME, SoundConfiguration.PARKOUR.COMPLETE_PITCH);
        restorePlayer(player, SettingsConfiguration.PARKOUR_BEHAVIOR.TELEPORT_TO_SPAWN_ON_FINISH);
        sessions.remove(player.getUniqueId());
    }

    @Override
    public void resetPlayer(Player player) {
        if (!hasActiveSession(player)) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NOT_IN_PARKOUR);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME, SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        Parkour parkour = getSession(player).getParkour();
        sessions.remove(player.getUniqueId());
        sessions.put(player.getUniqueId(), new ParkourSession(player, parkour));
        safeTeleport(player, parkour.getStartLocation().clone().add(0.5, 1, 0.5));
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.RESET_MESSAGE);
        playSound(player, SoundConfiguration.PARKOUR.RESET_SOUND,
                SoundConfiguration.PARKOUR.RESET_VOLUME, SoundConfiguration.PARKOUR.RESET_PITCH);
    }

    @Override
    public void teleportToCheckpoint(Player player) {
        ParkourSession session = getSession(player);
        if (session == null)
            return;

        Location loc = session.getLastCheckpointLocation();
        if (loc == null)
            loc = session.getParkour().getStartLocation();
        safeTeleport(player, loc.clone().add(0.5, 1, 0.5));
        playSound(player, SoundConfiguration.PARKOUR.CHECKPOINT_TP_SOUND,
                SoundConfiguration.PARKOUR.CHECKPOINT_TP_VOLUME,
                SoundConfiguration.PARKOUR.CHECKPOINT_TP_PITCH);
    }

    @Override
    public void quitParkour(Player player) {
        if (!hasActiveSession(player)) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NOT_IN_PARKOUR);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME, SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        sessions.remove(player.getUniqueId());
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.QUIT_MESSAGE);
        playSound(player, SoundConfiguration.PARKOUR.QUIT_SOUND,
                SoundConfiguration.PARKOUR.QUIT_VOLUME, SoundConfiguration.PARKOUR.QUIT_PITCH);
        restorePlayer(player, SettingsConfiguration.PARKOUR_BEHAVIOR.TELEPORT_TO_SPAWN_ON_QUIT);
    }

    @Override
    public boolean leaveParkour(Player player, boolean teleportToSpawn) {
        if (!hasActiveSession(player))
            return false;
        sessions.remove(player.getUniqueId());
        restorePlayer(player, teleportToSpawn);
        return true;
    }

    @Override
    public void handlePlayerQuit(Player player) {
        sessions.remove(player.getUniqueId());
        savedStates.remove(player.getUniqueId());
    }

    @Override
    public Parkour getParkourAtLocation(Location location) {
        String key = locationKey(location);
        Location cached = blockLocationCache.get(key);

        if (cached != null) {
            for (Parkour p : parkours.values()) {
                if (p.getStartLocation().getBlock().getLocation()
                        .equals(cached.getBlock().getLocation())) {
                    return p;
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasActiveSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    private void restorePlayer(Player player, boolean teleportToSpawn) {
        PlayerStateSnapshot snapshot = savedStates.remove(player.getUniqueId());
        boolean restored = false;

        if (snapshot != null) {
            snapshot.restore(player, SettingsConfiguration.PARKOUR_BEHAVIOR.RESTORE_SAVED_LOCATION
                    && !teleportToSpawn);
            restored = true;
        } else {
            hotbarService.giveLobbyHotbar(player);
        }

        player.updateInventory();
        if (teleportToSpawn || !restored)
            SpawnUtil.teleportToSpawn(player);
    }

    private void safeTeleport(Player player, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            SpawnUtil.teleportToSpawn(player);
            return;
        }

        Location target = loc.clone();
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.setFallDistance(0F);
        player.setVelocity(player.getVelocity().multiply(0));
        player.teleport(target);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setFallDistance(0F);
            player.setVelocity(player.getVelocity().multiply(0));
        }, 1L);
    }

    private void sendMessage(Player player, String... messages) {
        ColorUtil.sendMessage(player, "&b&l");
        for (String msg : messages)
            ColorUtil.sendMessage(player, msg);
        ColorUtil.sendMessage(player, "&a&l");
    }

    private void playSound(Player player, String sound, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(sound), volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String formatTime(long millis) {
        return String.format("%02d:%02d.%03d", millis / 60000,
                (millis % 60000) / 1000, millis % 1000);
    }

    @Override
    public void handleItemClick(Player player, ItemStack item) {
        if (item == null || !hasActiveSession(player))
            return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
            return;

        String name = item.getItemMeta().getDisplayName();
        if (name.equals(ColorUtil.color(
                center.bedwars.lobby.configuration.configurations.ItemsConfiguration.PARKOUR_HOTBAR.RESET.DISPLAY_NAME))) {
            resetPlayer(player);
        } else if (name.equals(ColorUtil.color(
                center.bedwars.lobby.configuration.configurations.ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME))) {
            teleportToCheckpoint(player);
        } else if (name.equals(ColorUtil.color(
                center.bedwars.lobby.configuration.configurations.ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME))) {
            quitParkour(player);
        }
    }
}

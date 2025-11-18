package center.bedwars.lobby.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.ItemsConfiguration;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.configuration.configurations.SoundConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.model.ParkourCheckpoint;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.parkour.session.ParkourSessionManager;
import center.bedwars.lobby.parkour.task.ParkourActionBarTask;
import center.bedwars.lobby.player.PlayerStateSnapshot;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
public class ParkourManager extends Manager {

    private final Map<String, Parkour> parkours = new HashMap<>();
    private final ParkourSessionManager sessionManager = new ParkourSessionManager();
    private final Map<UUID, Integer> playerCompletions = new HashMap<>();
    private final Map<UUID, PlayerStateSnapshot> savedStates = new HashMap<>();

    private DependencyManager dependencyManager;
    private HotbarManager hotbarManager;
    private IChunkAPI chunkAPI;
    private BukkitTask refreshTask;
    private BukkitTask actionBarTask;

    @Override
    protected void onLoad() {
        this.dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        this.hotbarManager = Lobby.getManagerStorage().getManager(HotbarManager.class);

        if (!dependencyManager.getCarbon().isApiAvailable()) {
            throw new IllegalStateException("Carbon dependency is required for ParkourManager");
        }

        if (!dependencyManager.getDecentHolograms().isPresent()) {
            throw new IllegalStateException("DecentHolograms dependency is required for ParkourManager");
        }

        this.chunkAPI = dependencyManager.getCarbon().getChunkRegistry();

        this.actionBarTask = new ParkourActionBarTask(this).runTaskTimer(Lobby.getINSTANCE(), 0L, 1L);

        scanAndInitializeParkours();
    }

    @Override
    protected void onUnload() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }

        if (actionBarTask != null) {
            actionBarTask.cancel();
        }

        parkours.values().forEach(parkour -> {
            if (parkour.getStartHologram() != null) {
                DHAPI.removeHologram(parkour.getStartHologram().getName());
            }
            parkour.getCheckpointHolograms().values().forEach(hologram -> DHAPI.removeHologram(hologram.getName()));
            if (parkour.getFinishHologram() != null) {
                DHAPI.removeHologram(parkour.getFinishHologram().getName());
            }
        });
        parkours.clear();
        sessionManager.clearAllSessions();
        playerCompletions.clear();
        savedStates.clear();
    }

    public void scheduleParkourRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }

        refreshTask = Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            Bukkit.getLogger().info("[ParkourManager] Refreshing parkours...");
            refreshParkours();
        }, 60L);
    }

    public void refreshParkours() {
        parkours.values().forEach(parkour -> {
            if (parkour.getStartHologram() != null) {
                DHAPI.removeHologram(parkour.getStartHologram().getName());
            }
            parkour.getCheckpointHolograms().values().forEach(hologram -> DHAPI.removeHologram(hologram.getName()));
            if (parkour.getFinishHologram() != null) {
                DHAPI.removeHologram(parkour.getFinishHologram().getName());
            }
        });
        parkours.clear();

        scanAndInitializeParkours();
    }

    private void scanAndInitializeParkours() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            return;
        }

        List<CompletableFuture<List<Block>>> chunkFutures = new ArrayList<>();

        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                final int chunkX = x;
                final int chunkZ = z;

                CompletableFuture<List<Block>> future = new CompletableFuture<>();
                chunkFutures.add(future);

                chunkAPI.getChunkAtAsync(world, chunkX, chunkZ, true, false, chunk -> {
                    List<Block> blocks = new ArrayList<>();

                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            for (int by = 0; by < world.getMaxHeight(); by++) {
                                Block block = chunk.getBlock(bx, by, bz);
                                Material type = block.getType();

                                if (type == Material.GOLD_BLOCK ||
                                        type == Material.IRON_BLOCK ||
                                        type == Material.DIAMOND_BLOCK) {
                                    blocks.add(block);
                                }
                            }
                        }
                    }

                    future.complete(blocks);
                });
            }
        }

        CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).thenRun(() -> Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            List<Block> allBlocks = chunkFutures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            processParkourBlocks(allBlocks);
            Bukkit.getLogger().info("[ParkourManager] Found " + parkours.size() + " parkour(s)!");
        }));
    }

    private void processParkourBlocks(List<Block> blocks) {
        Map<Location, Material> blockMap = new HashMap<>();
        blocks.forEach(block -> blockMap.put(block.getLocation(), block.getType()));

        Set<Location> processed = new HashSet<>();

        for (Block block : blocks) {
            if (processed.contains(block.getLocation())) {
                continue;
            }

            if (block.getType() == Material.GOLD_BLOCK) {
                Block plateAbove = block.getRelative(0, 1, 0);
                if (plateAbove.getType() == Material.WOOD_PLATE) {
                    Parkour parkour = buildParkourFromStart(block.getLocation(), blockMap, processed);
                    if (parkour != null) {
                        parkours.put(parkour.getId(), parkour);
                        createHolograms(parkour);
                        Bukkit.getLogger().info("[ParkourManager] Registered parkour: " + parkour.getId() +
                                " (Checkpoints: " + parkour.getCheckpoints().size() + ")");
                    }
                }
            }
        }
    }

    private Parkour buildParkourFromStart(Location startLocation, Map<Location, Material> blockMap, Set<Location> processed) {
        processed.add(startLocation);

        List<ParkourCheckpoint> checkpoints = new ArrayList<>();
        Location finishLocation = null;

        int searchRadius = 100;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location loc = startLocation.clone().add(x, y, z);
                    Material type = blockMap.get(loc);

                    if (type == Material.IRON_BLOCK) {
                        Block plateAbove = loc.getWorld().getBlockAt(loc).getRelative(0, 1, 0);
                        if (plateAbove.getType() == Material.WOOD_PLATE) {
                            checkpoints.add(new ParkourCheckpoint(0, loc.clone()));
                            processed.add(loc);
                        }
                    } else if (type == Material.DIAMOND_BLOCK) {
                        Block plateAbove = loc.getWorld().getBlockAt(loc).getRelative(0, 1, 0);
                        if (plateAbove.getType() == Material.WOOD_PLATE) {
                            finishLocation = loc.clone();
                            processed.add(loc);
                        }
                    }
                }
            }
        }

        if (finishLocation == null) {
            Bukkit.getLogger().warning("[ParkourManager] Parkour at " + startLocation + " has no finish block!");
            return null;
        }

        checkpoints.sort(Comparator.comparingDouble(cp -> cp.getLocation().distanceSquared(startLocation)));

        for (int i = 0; i < checkpoints.size(); i++) {
            ParkourCheckpoint old = checkpoints.get(i);
            checkpoints.set(i, new ParkourCheckpoint(i + 1, old.getLocation()));
        }

        String parkourId = "parkour_" + UUID.randomUUID().toString().substring(0, 8);
        return new Parkour(parkourId, startLocation, checkpoints, finishLocation);
    }

    private void createHolograms(Parkour parkour) {
        Location startLoc = parkour.getStartLocation().clone().add(0.5, 2.5, 0.5);
        Hologram startHologram = DHAPI.createHologram(parkour.getId() + "_start", startLoc);
        DHAPI.setHologramLines(startHologram, Arrays.asList(
                ColorUtil.color(LanguageConfiguration.HOLOGRAM.START_TITLE),
                ColorUtil.color(LanguageConfiguration.HOLOGRAM.START_SUBTITLE)
        ));
        parkour.setStartHologram(startHologram);

        for (ParkourCheckpoint checkpoint : parkour.getCheckpoints()) {
            Location checkpointLoc = checkpoint.getLocation().clone().add(0.5, 2.5, 0.5);
            Hologram checkpointHologram = DHAPI.createHologram(
                    parkour.getId() + "_checkpoint_" + checkpoint.getNumber(),
                    checkpointLoc
            );
            DHAPI.setHologramLines(checkpointHologram, Arrays.asList(
                    ColorUtil.color(LanguageConfiguration.HOLOGRAM.CHECKPOINT_TITLE),
                    ColorUtil.color(LanguageConfiguration.HOLOGRAM.CHECKPOINT_SUBTITLE)
            ));
            parkour.addCheckpointHologram(checkpoint.getNumber(), checkpointHologram);
        }

        Location finishLoc = parkour.getFinishLocation().clone().add(0.5, 2.5, 0.5);
        Hologram finishHologram = DHAPI.createHologram(parkour.getId() + "_finish", finishLoc);
        DHAPI.setHologramLines(finishHologram, Arrays.asList(
                ColorUtil.color(LanguageConfiguration.HOLOGRAM.FINISH_TITLE),
                ColorUtil.color(LanguageConfiguration.HOLOGRAM.FINISH_SUBTITLE)
        ));
        parkour.setFinishHologram(finishHologram);
    }

    public void startParkour(Player player, Parkour parkour) {
        ParkourSession existingSession = sessionManager.getSession(player);

        if (existingSession != null) {
            sessionManager.endSession(player);
            ParkourSession newSession = new ParkourSession(player, parkour);
            sessionManager.addSession(player, newSession);

            ColorUtil.sendMessage(player, "&b&l");
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.STARTED_TITLE);
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.CHECKPOINTS_INFO.replace("%checkpoints%", String.valueOf(parkour.getCheckpoints().size())));
            ColorUtil.sendMessage(player, "&a&l");

            playSound(player, SoundConfiguration.PARKOUR.START_SOUND,
                    SoundConfiguration.PARKOUR.START_VOLUME,
                    SoundConfiguration.PARKOUR.START_PITCH);
            return;
        }

        savedStates.put(player.getUniqueId(), PlayerStateSnapshot.capture(player));

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setAllowFlight(false);
        player.setFlying(false);

        hotbarManager.giveParkourHotbar(player);

        ParkourSession session = new ParkourSession(player, parkour);
        sessionManager.addSession(player, session);

        ColorUtil.sendMessage(player, "&b&l");
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.STARTED_TITLE);
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.CHECKPOINTS_INFO.replace("%checkpoints%", String.valueOf(parkour.getCheckpoints().size())));
        ColorUtil.sendMessage(player, "&a&l");

        playSound(player, SoundConfiguration.PARKOUR.START_SOUND,
                SoundConfiguration.PARKOUR.START_VOLUME,
                SoundConfiguration.PARKOUR.START_PITCH);
    }

    public void handleCheckpoint(Player player, Location location) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) {
            return;
        }

        ParkourCheckpoint checkpoint = session.getParkour().getCheckpointAt(location);
        if (checkpoint == null) {
            return;
        }

        if (session.hasReachedCheckpoint(checkpoint.getNumber())) {
            return;
        }

        session.reachCheckpoint(checkpoint.getNumber());
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.CHECKPOINT_REACHED);

        playSound(player, SoundConfiguration.PARKOUR.CHECKPOINT_SOUND,
                SoundConfiguration.PARKOUR.CHECKPOINT_VOLUME,
                SoundConfiguration.PARKOUR.CHECKPOINT_PITCH);
    }

    public void handleFinish(Player player, Location location) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) {
            return;
        }

        if (!session.getParkour().getFinishLocation().getBlock().getLocation().equals(location.getBlock().getLocation())) {
            return;
        }

        int checkpointsReached = session.getReachedCheckpoints().size();
        int totalCheckpoints = session.getParkour().getCheckpoints().size();

        if (checkpointsReached != totalCheckpoints) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NEED_ALL_CHECKPOINTS);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME,
                    SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        long timeTaken = session.getElapsedTime();

        int completions = playerCompletions.getOrDefault(player.getUniqueId(), 0) + 1;
        playerCompletions.put(player.getUniqueId(), completions);

        ColorUtil.sendMessage(player, "&c&l");
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.COMPLETED_TITLE);
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.TIME_MESSAGE.replace("%time%", formatTime(timeTaken)));
        ColorUtil.sendMessage(player, "&3&l");

        playSound(player, SoundConfiguration.PARKOUR.COMPLETE_SOUND,
                SoundConfiguration.PARKOUR.COMPLETE_VOLUME,
                SoundConfiguration.PARKOUR.COMPLETE_PITCH);

        restorePlayer(player, SettingsConfiguration.PARKOUR_BEHAVIOR.TELEPORT_TO_SPAWN_ON_FINISH);
        sessionManager.endSession(player);
    }

    public void resetPlayer(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NOT_IN_PARKOUR);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME,
                    SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        ParkourSession oldSession = sessionManager.getSession(player);
        Parkour parkour = oldSession.getParkour();

        sessionManager.endSession(player);
        ParkourSession newSession = new ParkourSession(player, parkour);
        sessionManager.addSession(player, newSession);

        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        Location startLoc = parkour.getStartLocation().clone().add(0.5, 1, 0.5);
        startLoc.setYaw(yaw);
        startLoc.setPitch(pitch);

        safeTeleport(player, startLoc);

        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.RESET_MESSAGE);
        playSound(player, SoundConfiguration.PARKOUR.RESET_SOUND,
                SoundConfiguration.PARKOUR.RESET_VOLUME,
                SoundConfiguration.PARKOUR.RESET_PITCH);
    }

    public void teleportToCheckpoint(Player player) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) {
            return;
        }

        Location spawnLoc = session.getLastCheckpointLocation();
        if (spawnLoc == null) {
            spawnLoc = session.getParkour().getStartLocation();
        }

        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        Location teleportLoc = spawnLoc.clone().add(0.5, 1, 0.5);
        teleportLoc.setYaw(yaw);
        teleportLoc.setPitch(pitch);

        safeTeleport(player, teleportLoc);

        playSound(player, SoundConfiguration.PARKOUR.CHECKPOINT_TP_SOUND,
                SoundConfiguration.PARKOUR.CHECKPOINT_TP_VOLUME,
                SoundConfiguration.PARKOUR.CHECKPOINT_TP_PITCH);
    }

    public void quitParkour(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NOT_IN_PARKOUR);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME,
                    SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        sessionManager.endSession(player);

        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.QUIT_MESSAGE);
        playSound(player, SoundConfiguration.PARKOUR.QUIT_SOUND,
                SoundConfiguration.PARKOUR.QUIT_VOLUME,
                SoundConfiguration.PARKOUR.QUIT_PITCH);

        restorePlayer(player, SettingsConfiguration.PARKOUR_BEHAVIOR.TELEPORT_TO_SPAWN_ON_QUIT);
    }

    public boolean leaveParkour(Player player, boolean teleportToSpawn) {
        if (!sessionManager.hasActiveSession(player)) {
            return false;
        }
        sessionManager.endSession(player);
        restorePlayer(player, teleportToSpawn);
        return true;
    }

    private void restorePlayer(Player player, boolean teleportToSpawn) {
        PlayerStateSnapshot snapshot = savedStates.remove(player.getUniqueId());
        boolean restored = false;

        if (snapshot != null) {
            boolean restoreLocation = SettingsConfiguration.PARKOUR_BEHAVIOR.RESTORE_SAVED_LOCATION && !teleportToSpawn;
            snapshot.restore(player, restoreLocation);
            restored = true;
        } else {
            hotbarManager.giveLobbyHotbar(player);
        }

        player.updateInventory();

        if (teleportToSpawn || !restored) {
            SpawnUtil.teleportToSpawn(player);
        }
    }

    private void safeTeleport(Player player, Location location) {
        player.setFallDistance(0F);
        player.setVelocity(player.getVelocity().multiply(0));
        player.teleport(location);

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            player.setFallDistance(0F);
            player.setVelocity(player.getVelocity().multiply(0));
        }, 1L);
    }

    public void handlePlayerQuit(Player player) {
        if (sessionManager.hasActiveSession(player)) {
            sessionManager.endSession(player);
        }
        savedStates.remove(player.getUniqueId());
    }

    public void handleItemClick(Player player, ItemStack item) {
        if (item == null || !sessionManager.hasActiveSession(player)) {
            return;
        }

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.RESET.DISPLAY_NAME))) {
            resetPlayer(player);
        } else if (displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME))) {
            teleportToCheckpoint(player);
        } else if (displayName.equals(ColorUtil.color(ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME))) {
            quitParkour(player);
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long millisRemaining = millis % 1000;

        return String.format("%02d:%02d.%03d", minutes, seconds, millisRemaining);
    }

    public Parkour getParkourAtLocation(Location location) {
        for (Parkour parkour : parkours.values()) {
            if (parkour.getStartLocation().getBlock().getLocation().equals(location.getBlock().getLocation())) {
                return parkour;
            }
        }
        return null;
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[ParkourManager] Invalid sound: " + soundName);
        }
    }
}
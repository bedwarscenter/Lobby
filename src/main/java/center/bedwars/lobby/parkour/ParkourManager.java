package center.bedwars.lobby.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.model.ParkourCheckpoint;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.parkour.session.ParkourSessionManager;
import center.bedwars.lobby.parkour.task.ParkourActionBarTask;
import center.bedwars.lobby.util.ColorUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

    private DependencyManager dependencyManager;
    private IChunkAPI chunkAPI;
    private BukkitTask refreshTask;
    private BukkitTask actionBarTask;

    @Override
    protected void onLoad() {
        this.dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);

        if (!dependencyManager.getCarbon().isApiAvailable()) {
            throw new IllegalStateException("Carbon dependency is required for ParkourManager");
        }

        if (!dependencyManager.getDecentHolograms().isPresent()) {
            throw new IllegalStateException("DecentHolograms dependency is required for ParkourManager");
        }

        this.chunkAPI = dependencyManager.getCarbon().getChunkRegistry();

        this.actionBarTask = new ParkourActionBarTask(this).runTaskTimer(Lobby.getINSTANCE(), 0L, 10L);

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
                ColorUtil.color("&6&lSTART"),
                ColorUtil.color("&7Step on the plate to begin")
        ));
        parkour.setStartHologram(startHologram);

        for (ParkourCheckpoint checkpoint : parkour.getCheckpoints()) {
            Location checkpointLoc = checkpoint.getLocation().clone().add(0.5, 2.5, 0.5);
            Hologram checkpointHologram = DHAPI.createHologram(
                    parkour.getId() + "_checkpoint_" + checkpoint.getNumber(),
                    checkpointLoc
            );
            DHAPI.setHologramLines(checkpointHologram, Collections.singletonList(
                    ColorUtil.color("&e&lCHECKPOINT")
            ));
            parkour.addCheckpointHologram(checkpoint.getNumber(), checkpointHologram);
        }

        Location finishLoc = parkour.getFinishLocation().clone().add(0.5, 2.5, 0.5);
        Hologram finishHologram = DHAPI.createHologram(parkour.getId() + "_finish", finishLoc);
        DHAPI.setHologramLines(finishHologram, Arrays.asList(
                ColorUtil.color("&a&lFINISH"),
                ColorUtil.color("&7Complete the parkour!")
        ));
        parkour.setFinishHologram(finishHologram);
    }

    public void startParkour(Player player, Parkour parkour) {
        if (sessionManager.hasActiveSession(player)) {
            return;
        }

        ParkourSession session = new ParkourSession(player, parkour);
        sessionManager.addSession(player, session);

        ColorUtil.sendMessage(player, "&6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        ColorUtil.sendMessage(player, "&6&lPARKOUR STARTED");
        ColorUtil.sendMessage(player, "&7Checkpoints: &e" + parkour.getCheckpoints().size());
        ColorUtil.sendMessage(player, "&6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
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
        ColorUtil.sendMessage(player, "&aCheckpoint reached!");
    }

    public void handleFinish(Player player, Location location) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) {
            return;
        }

        if (!session.getParkour().getFinishLocation().getBlock().getLocation().equals(location.getBlock().getLocation())) {
            return;
        }

        long timeTaken = session.getElapsedTime();
        int checkpointsReached = session.getReachedCheckpoints().size();
        int totalCheckpoints = session.getParkour().getCheckpoints().size();

        int completions = playerCompletions.getOrDefault(player.getUniqueId(), 0) + 1;
        playerCompletions.put(player.getUniqueId(), completions);

        ColorUtil.sendMessage(player, "&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        ColorUtil.sendMessage(player, "&a&lPARKOUR COMPLETED!");
        ColorUtil.sendMessage(player, "&eTime: &f" + formatTime(timeTaken));
        ColorUtil.sendMessage(player, "&eCheckpoints: &f" + checkpointsReached + "/" + totalCheckpoints);
        ColorUtil.sendMessage(player, "&eCompletions: &f" + completions);
        ColorUtil.sendMessage(player, "&a&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        sessionManager.endSession(player);
    }

    public void resetPlayer(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            ColorUtil.sendMessage(player, "&cYou are not in a parkour!");
            return;
        }

        sessionManager.endSession(player);
        ColorUtil.sendMessage(player, "&cParkour reset!");
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

        player.teleport(spawnLoc.clone().add(0.5, 1, 0.5));
    }

    public void quitParkour(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            ColorUtil.sendMessage(player, "&cYou are not in a parkour!");
            return;
        }

        sessionManager.endSession(player);
        ColorUtil.sendMessage(player, "&cParkour quit!");
    }

    public void handlePlayerQuit(Player player) {
        if (sessionManager.hasActiveSession(player)) {
            sessionManager.endSession(player);
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
}
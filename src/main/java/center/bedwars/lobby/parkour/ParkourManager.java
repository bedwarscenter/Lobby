package center.bedwars.lobby.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.model.ParkourCheckpoint;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.parkour.session.ParkourSessionManager;
import center.bedwars.lobby.parkour.util.ChatColor;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
        scanAndInitializeParkours();
    }

    @Override
    protected void onUnload() {
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
                            checkpoints.add(new ParkourCheckpoint(checkpoints.size() + 1, loc.clone()));
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
            return null;
        }

        checkpoints.sort(Comparator.comparingDouble(cp -> cp.getLocation().distanceSquared(startLocation)));

        String parkourId = "parkour_" + UUID.randomUUID().toString().substring(0, 8);
        return new Parkour(parkourId, startLocation, checkpoints, finishLocation);
    }

    private void createHolograms(Parkour parkour) {
        Location startLoc = parkour.getStartLocation().clone().add(0.5, 2.5, 0.5);
        Hologram startHologram = DHAPI.createHologram(parkour.getId() + "_start", startLoc);
        DHAPI.setHologramLines(startHologram, Collections.singletonList("&6&lSTART"));
        parkour.setStartHologram(startHologram);

        for (ParkourCheckpoint checkpoint : parkour.getCheckpoints()) {
            Location checkpointLoc = checkpoint.getLocation().clone().add(0.5, 2.5, 0.5);
            Hologram checkpointHologram = DHAPI.createHologram(
                    parkour.getId() + "_checkpoint_" + checkpoint.getNumber(),
                    checkpointLoc
            );
            DHAPI.setHologramLines(checkpointHologram, Collections.singletonList("&e&lCHECKPOINT"));
            parkour.addCheckpointHologram(checkpoint.getNumber(), checkpointHologram);
        }

        Location finishLoc = parkour.getFinishLocation().clone().add(0.5, 2.5, 0.5);
        Hologram finishHologram = DHAPI.createHologram(parkour.getId() + "_finish", finishLoc);
        DHAPI.setHologramLines(finishHologram, Collections.singletonList("&a&lEND"));
        parkour.setFinishHologram(finishHologram);
    }

    public void startParkour(Player player, Parkour parkour) {
        if (sessionManager.hasActiveSession(player)) {
            return;
        }

        ParkourSession session = new ParkourSession(player, parkour);
        sessionManager.addSession(player, session);

        player.sendMessage(ChatColor.translate("&aParkour started!"));
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
        player.sendMessage(ChatColor.translate("&aCheckpoint &e#" + checkpoint.getNumber() + " &areached!"));
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

        player.sendMessage(ChatColor.translate("&a&lPARKOUR COMPLETED!"));
        player.sendMessage(ChatColor.translate("&eTime: &f" + formatTime(timeTaken)));
        player.sendMessage(ChatColor.translate("&eCheckpoints: &f" + checkpointsReached + "/" + totalCheckpoints));
        player.sendMessage(ChatColor.translate("&eCompletions: &f" + completions));

        sessionManager.endSession(player);
    }

    public void resetPlayer(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            player.sendMessage(ChatColor.translate("&cYou are not in a parkour!"));
            return;
        }

        sessionManager.endSession(player);
        player.sendMessage(ChatColor.translate("&cParkour reset!"));
    }

    public void teleportToCheckpoint(Player player) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) {
            player.sendMessage(ChatColor.translate("&cYou are not in a parkour!"));
            return;
        }

        Location spawnLoc = session.getLastCheckpointLocation();
        if (spawnLoc == null) {
            spawnLoc = session.getParkour().getStartLocation();
        }

        player.teleport(spawnLoc.clone().add(0.5, 1, 0.5));
        player.sendMessage(ChatColor.translate("&aTeleported to checkpoint!"));
    }

    public void quitParkour(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            player.sendMessage(ChatColor.translate("&cYou are not in a parkour!"));
            return;
        }

        sessionManager.endSession(player);
        player.sendMessage(ChatColor.translate("&cParkour quit!"));
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
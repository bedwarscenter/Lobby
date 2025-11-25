package center.bedwars.lobby.parkour;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.configuration.configurations.SoundConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.manager.orphans.HotbarManager;
import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.model.ParkourCheckpoint;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.parkour.session.ParkourSessionManager;
import center.bedwars.lobby.parkour.task.ParkourActionBarTask;
import center.bedwars.lobby.player.PlayerStateSnapshot;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Getter
@SuppressWarnings("unused")
public class ParkourManager extends Manager {

    private final Map<String, Parkour> parkours = new HashMap<>();
    private final ParkourSessionManager sessionManager = new ParkourSessionManager();
    private final Map<UUID, Integer> playerCompletions = new HashMap<>();
    private final Map<UUID, PlayerStateSnapshot> savedStates = new HashMap<>();

    private HotbarManager hotbarManager;
    private IChunkAPI chunkAPI;
    private BukkitTask refreshTask;
    private BukkitTask actionBarTask;

    @Override
    protected void onLoad() {
        DependencyManager dm = Lobby.getManagerStorage().getManager(DependencyManager.class);
        this.hotbarManager = Lobby.getManagerStorage().getManager(HotbarManager.class);
        this.chunkAPI = dm.getCarbon().getChunkRegistry();
        this.actionBarTask = new ParkourActionBarTask(this).runTaskTimer(Lobby.getINSTANCE(), 0L, 1L);
        scanAndInitializeParkours();
    }

    @Override
    protected void onUnload() {
        if (refreshTask != null) refreshTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();
        parkours.values().forEach(this::removeHolograms);
        parkours.clear();
        sessionManager.clearAllSessions();
        playerCompletions.clear();
        savedStates.clear();
    }

    public void scheduleParkourRefresh() {
        if (refreshTask != null) refreshTask.cancel();
        refreshTask = Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), this::refreshParkours, 60L);
    }

    public void refreshParkours() {
        parkours.values().forEach(this::removeHolograms);
        parkours.clear();
        scanAndInitializeParkours();
    }

    private void scanAndInitializeParkours() {
        List<CompletableFuture<List<Block>>> futures = new ArrayList<>();

        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                CompletableFuture<List<Block>> future = new CompletableFuture<>();
                futures.add(future);
                chunkAPI.getChunkAtAsync(Bukkit.getWorld("world"), x, z, true, false,
                        chunk -> future.complete(scanChunkForBlocks(chunk)));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                    List<Block> allBlocks = new ArrayList<>();
                    futures.forEach(f -> allBlocks.addAll(f.join()));
                    processParkourBlocks(allBlocks);
                }));
    }

    private List<Block> scanChunkForBlocks(org.bukkit.Chunk chunk) {
        List<Block> blocks = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material type = block.getType();
                    if (type == Material.GOLD_BLOCK || type == Material.IRON_BLOCK ||
                            type == Material.DIAMOND_BLOCK) {
                        blocks.add(block);
                    }
                }
            }
        }
        return blocks;
    }

    private void processParkourBlocks(List<Block> blocks) {
        Map<Location, Material> blockMap = new HashMap<>();
        blocks.forEach(b -> blockMap.put(b.getLocation(), b.getType()));
        Set<Location> processed = new HashSet<>();

        for (Block block : blocks) {
            if (processed.contains(block.getLocation())) continue;
            if (block.getType() == Material.GOLD_BLOCK &&
                    block.getRelative(0, 1, 0).getType() == Material.WOOD_PLATE) {
                Parkour parkour = buildParkour(block.getLocation(), blockMap, processed);
                if (parkour != null) {
                    parkours.put(parkour.getId(), parkour);
                    createHolograms(parkour);
                }
            }
        }
    }

    private Parkour buildParkour(Location start, Map<Location, Material> blockMap, Set<Location> processed) {
        processed.add(start);
        List<ParkourCheckpoint> checkpoints = new ArrayList<>();
        Location finish = null;

        for (int x = -100; x <= 100; x++) {
            for (int y = -100; y <= 100; y++) {
                for (int z = -100; z <= 100; z++) {
                    Location loc = start.clone().add(x, y, z);
                    Material type = blockMap.get(loc);
                    if (type == null) continue;

                    Block plate = loc.getWorld().getBlockAt(loc).getRelative(0, 1, 0);
                    if (plate.getType() != Material.WOOD_PLATE) continue;

                    if (type == Material.IRON_BLOCK) {
                        checkpoints.add(new ParkourCheckpoint(0, loc.clone()));
                        processed.add(loc);
                    } else if (type == Material.DIAMOND_BLOCK) {
                        finish = loc.clone();
                        processed.add(loc);
                    }
                }
            }
        }

        if (finish == null) return null;

        checkpoints.sort(Comparator.comparingDouble(cp -> cp.getLocation().distanceSquared(start)));
        for (int i = 0; i < checkpoints.size(); i++) {
            checkpoints.set(i, new ParkourCheckpoint(i + 1, checkpoints.get(i).getLocation()));
        }

        return new Parkour("parkour_" + UUID.randomUUID().toString().substring(0, 8),
                start, checkpoints, finish);
    }

    private void createHolograms(Parkour parkour) {
        parkour.setStartHologram(createHologram(parkour.getId() + "_start",
                parkour.getStartLocation(), LanguageConfiguration.HOLOGRAM.START_TITLE,
                LanguageConfiguration.HOLOGRAM.START_SUBTITLE));

        for (ParkourCheckpoint cp : parkour.getCheckpoints()) {
            parkour.addCheckpointHologram(cp.getNumber(),
                    createHologram(parkour.getId() + "_cp_" + cp.getNumber(),
                            cp.getLocation(), LanguageConfiguration.HOLOGRAM.CHECKPOINT_TITLE,
                            LanguageConfiguration.HOLOGRAM.CHECKPOINT_SUBTITLE));
        }

        parkour.setFinishHologram(createHologram(parkour.getId() + "_finish",
                parkour.getFinishLocation(), LanguageConfiguration.HOLOGRAM.FINISH_TITLE,
                LanguageConfiguration.HOLOGRAM.FINISH_SUBTITLE));
    }

    private Hologram createHologram(String id, Location loc, String title, String subtitle) {
        Hologram h = DHAPI.createHologram(id, loc.clone().add(0.5, 2.5, 0.5));
        DHAPI.setHologramLines(h, Arrays.asList(ColorUtil.color(title), ColorUtil.color(subtitle)));
        return h;
    }

    private void removeHolograms(Parkour parkour) {
        if (parkour.getStartHologram() != null)
            DHAPI.removeHologram(parkour.getStartHologram().getName());
        parkour.getCheckpointHolograms().values()
                .forEach(h -> DHAPI.removeHologram(h.getName()));
        if (parkour.getFinishHologram() != null)
            DHAPI.removeHologram(parkour.getFinishHologram().getName());
    }

    public void startParkour(Player player, Parkour parkour) {
        ParkourSession existing = sessionManager.getSession(player);

        if (existing != null) {
            sessionManager.endSession(player);
            sessionManager.addSession(player, new ParkourSession(player, parkour));
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
        hotbarManager.giveParkourHotbar(player);
        sessionManager.addSession(player, new ParkourSession(player, parkour));
        sendMessage(player, LanguageConfiguration.PARKOUR.STARTED_TITLE,
                LanguageConfiguration.PARKOUR.CHECKPOINTS_INFO
                        .replace("%checkpoints%", String.valueOf(parkour.getCheckpoints().size())));
        playSound(player, SoundConfiguration.PARKOUR.START_SOUND,
                SoundConfiguration.PARKOUR.START_VOLUME, SoundConfiguration.PARKOUR.START_PITCH);
    }

    public void handleCheckpoint(Player player, Location location) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) return;

        ParkourCheckpoint cp = session.getParkour().getCheckpointAt(location);
        if (cp == null || session.hasReachedCheckpoint(cp.getNumber())) return;

        session.reachCheckpoint(cp.getNumber());
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.CHECKPOINT_REACHED);
        playSound(player, SoundConfiguration.PARKOUR.CHECKPOINT_SOUND,
                SoundConfiguration.PARKOUR.CHECKPOINT_VOLUME, SoundConfiguration.PARKOUR.CHECKPOINT_PITCH);
    }

    public void handleFinish(Player player, Location location) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) return;
        if (!session.getParkour().getFinishLocation().getBlock().getLocation()
                .equals(location.getBlock().getLocation())) return;

        if (session.getReachedCheckpoints().size() !=
                session.getParkour().getCheckpoints().size()) {
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
        sessionManager.endSession(player);
    }

    public void resetPlayer(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NOT_IN_PARKOUR);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME, SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        Parkour parkour = sessionManager.getSession(player).getParkour();
        sessionManager.endSession(player);
        sessionManager.addSession(player, new ParkourSession(player, parkour));
        safeTeleport(player, parkour.getStartLocation().clone().add(0.5, 1, 0.5));
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.RESET_MESSAGE);
        playSound(player, SoundConfiguration.PARKOUR.RESET_SOUND,
                SoundConfiguration.PARKOUR.RESET_VOLUME, SoundConfiguration.PARKOUR.RESET_PITCH);
    }

    public void teleportToCheckpoint(Player player) {
        ParkourSession session = sessionManager.getSession(player);
        if (session == null) return;

        Location loc = session.getLastCheckpointLocation();
        if (loc == null) loc = session.getParkour().getStartLocation();
        safeTeleport(player, loc.clone().add(0.5, 1, 0.5));
        playSound(player, SoundConfiguration.PARKOUR.CHECKPOINT_TP_SOUND,
                SoundConfiguration.PARKOUR.CHECKPOINT_TP_VOLUME,
                SoundConfiguration.PARKOUR.CHECKPOINT_TP_PITCH);
    }

    public void quitParkour(Player player) {
        if (!sessionManager.hasActiveSession(player)) {
            ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.NOT_IN_PARKOUR);
            playSound(player, SoundConfiguration.PARKOUR.ERROR_SOUND,
                    SoundConfiguration.PARKOUR.ERROR_VOLUME, SoundConfiguration.PARKOUR.ERROR_PITCH);
            return;
        }

        sessionManager.endSession(player);
        ColorUtil.sendMessage(player, LanguageConfiguration.PARKOUR.QUIT_MESSAGE);
        playSound(player, SoundConfiguration.PARKOUR.QUIT_SOUND,
                SoundConfiguration.PARKOUR.QUIT_VOLUME, SoundConfiguration.PARKOUR.QUIT_PITCH);
        restorePlayer(player, SettingsConfiguration.PARKOUR_BEHAVIOR.TELEPORT_TO_SPAWN_ON_QUIT);
    }

    public boolean leaveParkour(Player player, boolean teleportToSpawn) {
        if (!sessionManager.hasActiveSession(player)) return false;
        sessionManager.endSession(player);
        restorePlayer(player, teleportToSpawn);
        return true;
    }

    private void restorePlayer(Player player, boolean teleportToSpawn) {
        PlayerStateSnapshot snapshot = savedStates.remove(player.getUniqueId());
        boolean restored = false;

        if (snapshot != null) {
            snapshot.restore(player, SettingsConfiguration.PARKOUR_BEHAVIOR.RESTORE_SAVED_LOCATION
                    && !teleportToSpawn);
            restored = true;
        } else {
            hotbarManager.giveLobbyHotbar(player);
        }

        player.updateInventory();
        if (teleportToSpawn || !restored) SpawnUtil.teleportToSpawn(player);
    }

    private void safeTeleport(Player player, Location loc) {
        Location target = loc.clone();
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.setFallDistance(0F);
        player.setVelocity(player.getVelocity().multiply(0));
        player.teleport(target);
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            player.setFallDistance(0F);
            player.setVelocity(player.getVelocity().multiply(0));
        }, 1L);
    }

    public void handlePlayerQuit(Player player) {
        sessionManager.endSession(player);
        savedStates.remove(player.getUniqueId());
    }

    public void handleItemClick(Player player, ItemStack item) {
        if (item == null || !sessionManager.hasActiveSession(player)) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = item.getItemMeta().getDisplayName();
        if (name.equals(ColorUtil.color(center.bedwars.lobby.configuration.configurations.ItemsConfiguration
                .PARKOUR_HOTBAR.RESET.DISPLAY_NAME))) {
            resetPlayer(player);
        } else if (name.equals(ColorUtil.color(center.bedwars.lobby.configuration.configurations
                .ItemsConfiguration.PARKOUR_HOTBAR.CHECKPOINT.DISPLAY_NAME))) {
            teleportToCheckpoint(player);
        } else if (name.equals(ColorUtil.color(center.bedwars.lobby.configuration.configurations
                .ItemsConfiguration.PARKOUR_HOTBAR.EXIT.DISPLAY_NAME))) {
            quitParkour(player);
        }
    }

    public Parkour getParkourAtLocation(Location location) {
        for (Parkour p : parkours.values()) {
            if (p.getStartLocation().getBlock().getLocation()
                    .equals(location.getBlock().getLocation())) return p;
        }
        return null;
    }

    private void sendMessage(Player player, String... messages) {
        ColorUtil.sendMessage(player, "&b&l");
        for (String msg : messages) ColorUtil.sendMessage(player, msg);
        ColorUtil.sendMessage(player, "&a&l");
    }

    private void playSound(Player player, String sound, float volume, float pitch) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(sound), volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    private String formatTime(long millis) {
        return String.format("%02d:%02d.%03d", millis / 60000,
                (millis % 60000) / 1000, millis % 1000);
    }
}
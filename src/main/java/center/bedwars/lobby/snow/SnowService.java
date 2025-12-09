package center.bedwars.lobby.snow;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.IMongoService;
import center.bedwars.lobby.service.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.model.Filters;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SnowService extends AbstractService implements ISnowService {

    private static final String COLLECTION_NAME = "player_snow_settings";

    private final Lobby plugin;
    private final IMongoService mongoService;
    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Boolean> playerDataLoaded = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMoveTime = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private BukkitTask particleTask;

    @Inject
    public SnowService(Lobby plugin, IMongoService mongoService) {
        this.plugin = plugin;
        this.mongoService = mongoService;
    }

    @Override
    protected void onEnable() {
        if (!SettingsConfiguration.SNOW_RAIN.ENABLED) {
            plugin.getLogger().info("Snow effect system is disabled in config");
            return;
        }
        startParticleTask();
    }

    @Override
    protected void onDisable() {
        if (particleTask != null) {
            particleTask.cancel();
        }
        enabledPlayers.clear();
        playerDataLoaded.clear();
        lastMoveTime.clear();
    }

    private void startParticleTask() {
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : enabledPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    spawnFallingSnow(player);
                    spawnGroundSnow(player);
                }
            }
        }, 0L, SettingsConfiguration.SNOW_RAIN.UPDATE_INTERVAL);
    }

    private void spawnFallingSnow(Player player) {
        Location loc = player.getLocation();
        double radius = SettingsConfiguration.SNOW_RAIN.RADIUS;
        double height = SettingsConfiguration.SNOW_RAIN.HEIGHT;
        int count = SettingsConfiguration.SNOW_RAIN.PARTICLE_COUNT;

        for (int i = 0; i < count; i++) {
            double x = loc.getX() + (random.nextDouble() * 2 - 1) * radius;
            double y = loc.getY() + height + random.nextDouble() * 5;
            double z = loc.getZ() + (random.nextDouble() * 2 - 1) * radius;

            float offsetX = (float) (random.nextDouble() * 0.02 - 0.01);
            float offsetY = (float) (-0.08 - random.nextDouble() * 0.04);
            float offsetZ = (float) (random.nextDouble() * 0.02 - 0.01);

            sendParticle(player, EnumParticle.FIREWORKS_SPARK, x, y, z, offsetX, offsetY, offsetZ, 0.005f, 1);
        }
    }

    private void spawnGroundSnow(Player player) {
        Location loc = player.getLocation();
        double radius = SettingsConfiguration.SNOW_RAIN.RADIUS;
        int groundParticles = SettingsConfiguration.SNOW_RAIN.PARTICLE_COUNT / 3;

        for (int i = 0; i < groundParticles; i++) {
            double x = loc.getX() + (random.nextDouble() * 2 - 1) * radius;
            double z = loc.getZ() + (random.nextDouble() * 2 - 1) * radius;

            Location groundLoc = new Location(loc.getWorld(), x, loc.getY(), z);
            Block groundBlock = getGroundBlock(groundLoc);

            if (groundBlock != null && groundBlock.getType() != Material.AIR) {
                double y = groundBlock.getY() + 1.05;
                sendParticle(player, EnumParticle.SNOW_SHOVEL, x, y, z, 0.3f, 0.02f, 0.3f, 0.001f, 1);
            }
        }
    }

    private Block getGroundBlock(Location loc) {
        Location checkLoc = loc.clone();
        for (int i = 0; i < 20; i++) {
            Block block = checkLoc.getBlock();
            if (block.getType() != Material.AIR) {
                return block;
            }
            checkLoc.subtract(0, 1, 0);
            if (checkLoc.getY() < 0)
                break;
        }
        return null;
    }

    @Override
    public void onPlayerMove(Player player) {
        if (!SettingsConfiguration.SNOW_RAIN.ENABLED)
            return;
        if (!enabledPlayers.contains(player.getUniqueId()))
            return;

        long now = System.currentTimeMillis();
        Long lastMove = lastMoveTime.get(player.getUniqueId());

        if (lastMove != null && now - lastMove < 100)
            return;
        lastMoveTime.put(player.getUniqueId(), now);

        spawnSnowSplash(player);
    }

    private void spawnSnowSplash(Player player) {
        Location loc = player.getLocation();

        for (int i = 0; i < 8; i++) {
            double x = loc.getX() + (random.nextDouble() * 1.0 - 0.5);
            double y = loc.getY() + 0.1 + random.nextDouble() * 0.3;
            double z = loc.getZ() + (random.nextDouble() * 1.0 - 0.5);

            float offsetX = (float) (random.nextDouble() * 0.3 - 0.15);
            float offsetY = (float) (0.1 + random.nextDouble() * 0.2);
            float offsetZ = (float) (random.nextDouble() * 0.3 - 0.15);

            sendParticle(player, EnumParticle.SNOW_SHOVEL, x, y, z, offsetX, offsetY, offsetZ, 0.05f, 1);
        }

        for (int i = 0; i < 4; i++) {
            double x = loc.getX() + (random.nextDouble() * 0.6 - 0.3);
            double y = loc.getY() + 0.05;
            double z = loc.getZ() + (random.nextDouble() * 0.6 - 0.3);

            sendParticle(player, EnumParticle.FIREWORKS_SPARK, x, y, z,
                    (float) (random.nextDouble() * 0.2 - 0.1),
                    (float) (0.05 + random.nextDouble() * 0.1),
                    (float) (random.nextDouble() * 0.2 - 0.1),
                    0.02f, 1);
        }
    }

    private void sendParticle(Player player, EnumParticle particle, double x, double y, double z,
            float offsetX, float offsetY, float offsetZ, float speed, int count) {
        try {
            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                    particle,
                    true,
                    (float) x, (float) y, (float) z,
                    offsetX, offsetY, offsetZ,
                    speed,
                    count);
            center.bedwars.lobby.nms.NMSHelper.sendPacket(player, packet);
        } catch (Exception e) {
        }
    }

    @Override
    public void toggleSnow(Player player) {
        if (!SettingsConfiguration.SNOW_RAIN.ENABLED) {
            player.sendMessage(color(SettingsConfiguration.SNOW_RAIN.MESSAGES.FEATURE_DISABLED));
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean enabled = enabledPlayers.contains(uuid);

        if (enabled) {
            enabledPlayers.remove(uuid);
            player.sendMessage(color(SettingsConfiguration.SNOW_RAIN.MESSAGES.DISABLED_MESSAGE));
        } else {
            enabledPlayers.add(uuid);
            player.sendMessage(color(SettingsConfiguration.SNOW_RAIN.MESSAGES.ENABLED_MESSAGE));
        }

        savePlayerSetting(uuid, !enabled);
    }

    @Override
    public boolean hasSnowEnabled(UUID playerUuid) {
        return enabledPlayers.contains(playerUuid);
    }

    @Override
    public void onPlayerJoin(Player player) {
        if (!SettingsConfiguration.SNOW_RAIN.ENABLED)
            return;

        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Boolean setting = loadPlayerSetting(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline())
                    return;

                if (setting == null) {
                    playerDataLoaded.put(uuid, false);
                    int delayTicks = SettingsConfiguration.SNOW_RAIN.PROMPT_DELAY_SECONDS * 20;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline() && !playerDataLoaded.getOrDefault(uuid, true)) {
                            player.sendMessage(color(SettingsConfiguration.SNOW_RAIN.MESSAGES.PROMPT_MESSAGE));
                        }
                    }, delayTicks);
                } else {
                    playerDataLoaded.put(uuid, true);
                    if (setting) {
                        enabledPlayers.add(uuid);
                    }
                }
            });
        });
    }

    @Override
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        enabledPlayers.remove(uuid);
        playerDataLoaded.remove(uuid);
        lastMoveTime.remove(uuid);
    }

    private Boolean loadPlayerSetting(UUID uuid) {
        try {
            Document doc = mongoService.findOneAsync(COLLECTION_NAME, Filters.eq("uuid", uuid.toString())).join();
            if (doc != null) {
                return doc.getBoolean("enabled", false);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load snow setting for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    private void savePlayerSetting(UUID uuid, boolean enabled) {
        Document doc = new Document("uuid", uuid.toString())
                .append("enabled", enabled)
                .append("updated_at", System.currentTimeMillis());

        mongoService.deleteAsync(COLLECTION_NAME, Filters.eq("uuid", uuid.toString()))
                .thenCompose(v -> mongoService.insertAsync(COLLECTION_NAME, doc))
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        plugin.getLogger().warning("Failed to save snow setting for " + uuid + ": " + ex.getMessage());
                    } else {
                        playerDataLoaded.put(uuid, true);
                    }
                });
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.IRedisService;
import center.bedwars.lobby.service.AbstractService;
import center.bedwars.lobby.sync.fake.FakePlayer;
import center.bedwars.lobby.sync.fake.FakePlayerManager;
import center.bedwars.lobby.sync.serialization.PlayerSerializer;
import center.bedwars.lobby.sync.serialization.PlayerSerializer.PlayerSyncAction;
import center.bedwars.lobby.sync.serialization.PlayerSerializer.PlayerSyncPacket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

@Singleton
public class PlayerSyncService extends AbstractService implements IPlayerSyncService {

    private static final String REDIS_CHANNEL = "bwl:ps";
    private static final int HEARTBEAT_INTERVAL_SEC = 5;
    private static final int POSITION_SYNC_INTERVAL_MS = 50;
    private static final int TIMEOUT_MS = 20000;

    private final Lobby plugin;
    private final IRedisService redisService;

    @Getter
    private final FakePlayerManager fakePlayerManager;

    private byte lobbyId;
    private final Map<UUID, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile boolean running = false;

    @Inject
    public PlayerSyncService(Lobby plugin, IRedisService redisService, FakePlayerManager fakePlayerManager) {
        this.plugin = plugin;
        this.redisService = redisService;
        this.fakePlayerManager = fakePlayerManager;
    }

    @Override
    protected void onEnable() {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED) {
            plugin.getLogger().info("Player sync is disabled in config");
            return;
        }
        this.lobbyId = getLobbyIdAsByte(SettingsConfiguration.LOBBY_ID);
        running = true;
        setupSubscription();
        startHeartbeat();
    }

    @Override
    protected void onDisable() {
        running = false;
        executor.shutdownNow();

        fakePlayerManager.cleanup();

        for (UUID id : new ArrayList<>(remotePlayers.keySet())) {
            removeFromTab(id);
        }
        remotePlayers.clear();
    }

    private void setupSubscription() {
        redisService.subscribe(REDIS_CHANNEL, this::processMessage);
    }

    private void startHeartbeat() {
        executor.scheduleAtFixedRate(() -> {
            if (!running)
                return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                String posData = String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f,%.2f",
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                broadcast(player, PlayerSyncAction.HEARTBEAT, posData);
            }
            cleanup();
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void startPositionSync() {
        executor.scheduleAtFixedRate(() -> {
            if (!running)
                return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                String posData = String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f,%.2f,%b",
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), player.isOnGround());
                broadcast(player, PlayerSyncAction.POSITION, posData);
            }
        }, POSITION_SYNC_INTERVAL_MS, POSITION_SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void processMessage(byte[] raw) {
        try {
            PlayerSyncPacket packet = PlayerSerializer.deserialize(raw);

            if (lobbyId == packet.lobbyId)
                return;

            UUID id = packet.playerId;

            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (packet.action) {
                    case JOIN:
                        handleJoin(id, packet.lobbyId, packet.name, packet.texture, packet.signature, packet.data);
                        break;
                    case QUIT:
                        handleQuit(id);
                        break;
                    case HEARTBEAT:
                        handleHeartbeat(id, packet.lobbyId, packet.name, packet.texture, packet.signature, packet.data);
                        break;
                    case POSITION:
                        handlePosition(id, packet.data);
                        break;
                    case LOOK:
                        handleLook(id, packet.data);
                        break;
                    case ANIMATION:
                        handleAnimation(id, packet.data);
                        break;
                    case SNEAK:
                        handleSneak(id, packet.data);
                        break;
                    case SPRINT:
                        handleSprint(id, packet.data);
                        break;
                    case FLY:
                        handleFly(id, packet.data);
                        break;
                    case HELD_ITEM:
                        handleHeldItem(id, packet.data);
                        break;
                    case DAMAGE:
                        handleDamage(id);
                        break;
                    case EQUIPMENT:
                    case METADATA:
                    case VISIBILITY:
                        break;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleJoin(UUID id, byte sourceLobby, String name, String texture, String signature, String posData) {
        if (Bukkit.getPlayer(id) != null) {
            return;
        }

        RemotePlayer rp = remotePlayers.get(id);
        if (rp != null) {
            rp.sourceLobbyId = sourceLobby;
            rp.lastUpdate = System.currentTimeMillis();
            return;
        }

        rp = new RemotePlayer(id, name, texture, signature, sourceLobby);
        remotePlayers.put(id, rp);

        for (Player player : Bukkit.getOnlinePlayers()) {
            addToTab(player, rp);
        }

        double x = 0, y = 64, z = 0;
        float yaw = 0, pitch = 0;
        if (posData != null && !posData.isEmpty()) {
            try {
                String[] parts = posData.split(",");
                if (parts.length >= 5) {
                    x = Double.parseDouble(parts[0]);
                    y = Double.parseDouble(parts[1]);
                    z = Double.parseDouble(parts[2]);
                    yaw = Float.parseFloat(parts[3]);
                    pitch = Float.parseFloat(parts[4]);
                }
            } catch (Exception ignored) {
            }
        }
        fakePlayerManager.createFakePlayer(id, name, texture, signature, sourceLobby, x, y, z, yaw, pitch);
    }

    private void handleQuit(UUID id) {
        remotePlayers.remove(id);
        removeFromTab(id);
        fakePlayerManager.removeFakePlayer(id);
    }

    private void handleHeartbeat(UUID id, byte sourceLobby, String name, String texture, String signature,
            String posData) {
        RemotePlayer rp = remotePlayers.get(id);
        if (rp == null) {
            handleJoin(id, sourceLobby, name, texture, signature, posData);
        } else {
            rp.lastUpdate = System.currentTimeMillis();
            rp.sourceLobbyId = sourceLobby;

            FakePlayer fake = fakePlayerManager.getFakePlayer(id);
            if (fake != null) {
                fake.setLastUpdate(System.currentTimeMillis());
                if (posData != null && !posData.isEmpty()) {
                    try {
                        String[] parts = posData.split(",");
                        if (parts.length >= 5) {
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            double z = Double.parseDouble(parts[2]);
                            float yaw = Float.parseFloat(parts[3]);
                            float pitch = Float.parseFloat(parts[4]);
                            fakePlayerManager.updatePosition(id, x, y, z, yaw, pitch, false);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void handlePosition(UUID id, String data) {
        if (data == null || data.isEmpty())
            return;

        try {
            String[] parts = data.split(",");
            if (parts.length < 5)
                return;

            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = Float.parseFloat(parts[3]);
            float pitch = Float.parseFloat(parts[4]);
            boolean onGround = parts.length > 5 && Boolean.parseBoolean(parts[5]);

            fakePlayerManager.updatePosition(id, x, y, z, yaw, pitch, onGround);

            RemotePlayer rp = remotePlayers.get(id);
            if (rp != null) {
                rp.lastUpdate = System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLook(UUID id, String data) {
        if (data == null || data.isEmpty())
            return;

        try {
            String[] parts = data.split(",");
            if (parts.length < 2)
                return;

            float yaw = Float.parseFloat(parts[0]);
            float pitch = Float.parseFloat(parts[1]);

            fakePlayerManager.updateLook(id, yaw, pitch);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAnimation(UUID id, String data) {
        int animationType = 0;
        if (data != null && !data.isEmpty()) {
            try {
                animationType = Integer.parseInt(data);
            } catch (NumberFormatException ignored) {
            }
        }
        fakePlayerManager.playAnimation(id, animationType);
    }

    private void handleSneak(UUID id, String data) {
        boolean sneaking = "true".equalsIgnoreCase(data);
        fakePlayerManager.updateSneaking(id, sneaking);
    }

    private void handleSprint(UUID id, String data) {
        boolean sprinting = "true".equalsIgnoreCase(data);
        fakePlayerManager.updateSprinting(id, sprinting);
    }

    private void handleFly(UUID id, String data) {
        boolean flying = "true".equalsIgnoreCase(data);
        fakePlayerManager.updateFlying(id, flying);
    }

    private void handleHeldItem(UUID id, String data) {
        int slot = 0;
        if (data != null && !data.isEmpty()) {
            try {
                slot = Integer.parseInt(data);
            } catch (NumberFormatException ignored) {
            }
        }
        fakePlayerManager.updateHeldItem(id, slot);
    }

    private void handleDamage(UUID id) {
        fakePlayerManager.playAnimation(id, 1);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, RemotePlayer> e : remotePlayers.entrySet()) {
            if (now - e.getValue().lastUpdate > TIMEOUT_MS) {
                toRemove.add(e.getKey());
            }
        }

        if (!toRemove.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID id : toRemove) {
                    handleQuit(id);
                }
            });
        }

        fakePlayerManager.cleanupStale(TIMEOUT_MS);
    }

    @Override
    public void handlePlayerJoin(Player player) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        Location loc = player.getLocation();
        String posData = String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f,%.2f",
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        broadcast(player, PlayerSyncAction.JOIN, posData);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                for (RemotePlayer rp : remotePlayers.values()) {
                    addToTab(player, rp);
                }
                fakePlayerManager.spawnAllFor(player);
            }
        }, 30L);
    }

    @Override
    public void handlePlayerQuit(Player player) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        broadcast(player, PlayerSyncAction.QUIT, null);
        fakePlayerManager.despawnAllFor(player);
    }

    @Override
    public void broadcast(Player player, PlayerSyncAction action, String data) {
        if (!SettingsConfiguration.PLAYER_SYNC.ENABLED)
            return;
        CompletableFuture.runAsync(() -> {
            try {
                CraftPlayer craftPlayer = (CraftPlayer) player;
                GameProfile profile = craftPlayer.getProfile();

                String texture = "";
                String signature = "";

                if (profile.getProperties().containsKey("textures")) {
                    Property textureProp = profile.getProperties().get("textures").iterator().next();
                    texture = textureProp.getValue();
                    signature = textureProp.getSignature() != null ? textureProp.getSignature() : "";
                }

                PlayerSyncPacket packet = new PlayerSyncPacket(
                        lobbyId,
                        action,
                        player.getUniqueId(),
                        player.getName(),
                        texture,
                        signature,
                        data != null ? data : "");

                byte[] serialized = PlayerSerializer.serialize(packet);
                redisService.publish(REDIS_CHANNEL, serialized);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void addToTab(Player player, RemotePlayer rp) {
        GameProfile profile = new GameProfile(rp.uuid, rp.name);
        if (rp.texture != null && !rp.texture.isEmpty()) {
            profile.getProperties().put("textures", new Property("textures", rp.texture, rp.signature));
        }

        EntityPlayer entityPlayer = new EntityPlayer(
                ((CraftPlayer) player).getHandle().server,
                (WorldServer) ((CraftPlayer) player).getHandle().world,
                profile,
                new PlayerInteractManager(((CraftPlayer) player).getHandle().world));

        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    private void removeFromTab(UUID id) {
        if (Bukkit.getOnlinePlayers().isEmpty())
            return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            GameProfile profile = new GameProfile(id, "");
            EntityPlayer entityPlayer = new EntityPlayer(
                    ((CraftPlayer) player).getHandle().server,
                    (WorldServer) ((CraftPlayer) player).getHandle().world,
                    profile,
                    new PlayerInteractManager(((CraftPlayer) player).getHandle().world));

            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    private byte getLobbyIdAsByte(String id) {
        try {
            return Byte.parseByte(id.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return (byte) id.hashCode();
        }
    }

    public Map<UUID, RemotePlayer> getRemotePlayers() {
        return Collections.unmodifiableMap(remotePlayers);
    }

    public static class RemotePlayer {
        public UUID uuid;
        public String name;
        public String texture;
        public String signature;
        public byte sourceLobbyId;
        public long lastUpdate;

        RemotePlayer(UUID uuid, String name, String texture, String signature, byte sourceLobbyId) {
            this.uuid = uuid;
            this.name = name;
            this.texture = texture;
            this.signature = signature;
            this.sourceLobbyId = sourceLobbyId;
            this.lastUpdate = System.currentTimeMillis();
        }
    }
}

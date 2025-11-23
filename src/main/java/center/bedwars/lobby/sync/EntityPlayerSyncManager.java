package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.zip.Deflater;

@Getter
public class EntityPlayerSyncManager extends Manager {

    private static final String REDIS_CHANNEL = "bwl:ep";
    private static final Gson GSON = new Gson();
    private static final int UPDATE_INTERVAL_MS = 100;
    private static final int CLEANUP_INTERVAL_SEC = 2;
    private static final int ENTITY_TIMEOUT_MS = 3000;
    private static final double POSITION_THRESHOLD = 0.5;
    private static final float ROTATION_THRESHOLD = 5.0f;

    private final String lobbyId = SettingsConfiguration.LOBBY_ID;
    private RedisDatabase redis;
    private final Map<UUID, RemoteEntity> entities = new ConcurrentHashMap<>();
    private final Map<UUID, CachedState> stateCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final BlockingQueue<CompressedUpdate> updateQueue = new LinkedBlockingQueue<>(500);

    @Override
    protected void onLoad() {
        this.redis = Lobby.getManagerStorage().getManager(DatabaseManager.class).getRedis();
        setupSubscription();
        startUpdateBroadcaster();
        startCleanupTask();
    }

    @Override
    protected void onUnload() {
        executor.shutdownNow();
        entities.values().forEach(RemoteEntity::destroy);
        entities.clear();
        stateCache.clear();
        updateQueue.clear();
    }

    private void setupSubscription() {
        redis.subscribe(REDIS_CHANNEL, this::processCompressedMessage);
    }

    private void startUpdateBroadcaster() {
        executor.scheduleAtFixedRate(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                queueUpdate(player);
            }
            flushQueue();
        }, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void startCleanupTask() {
        executor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            entities.entrySet().removeIf(entry -> {
                if (now - entry.getValue().lastUpdate > ENTITY_TIMEOUT_MS) {
                    Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), entry.getValue()::destroy);
                    return true;
                }
                return false;
            });
        }, CLEANUP_INTERVAL_SEC, CLEANUP_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void queueUpdate(Player player) {
        CachedState cached = stateCache.get(player.getUniqueId());
        Location loc = player.getLocation();

        if (cached != null && !hasSignificantChange(cached, loc)) {
            return;
        }

        byte[] compressed = compressPlayerData(player, loc);
        if (compressed != null) {
            updateQueue.offer(new CompressedUpdate(player.getUniqueId(), compressed));
            stateCache.put(player.getUniqueId(), new CachedState(loc));
        }
    }

    private boolean hasSignificantChange(CachedState cached, Location current) {
        return cached.location.distanceSquared(current) > POSITION_THRESHOLD * POSITION_THRESHOLD ||
                Math.abs(cached.location.getYaw() - current.getYaw()) > ROTATION_THRESHOLD ||
                Math.abs(cached.location.getPitch() - current.getPitch()) > ROTATION_THRESHOLD;
    }

    private byte[] compressPlayerData(Player player, Location loc) {
        try {
            StringBuilder sb = new StringBuilder(256);
            sb.append(lobbyId).append('|')
                    .append(player.getUniqueId()).append('|')
                    .append(player.getName()).append('|')
                    .append(String.format("%.2f", loc.getX())).append(',')
                    .append(String.format("%.2f", loc.getY())).append(',')
                    .append(String.format("%.2f", loc.getZ())).append(',')
                    .append((int)loc.getYaw()).append(',')
                    .append((int)loc.getPitch()).append('|')
                    .append(player.isSneaking() ? '1' : '0')
                    .append(player.isSprinting() ? '1' : '0')
                    .append('|')
                    .append(player.getInventory().getHeldItemSlot());

            EntityPlayer nms = ((CraftPlayer) player).getHandle();
            GameProfile profile = nms.getProfile();
            if (profile.getProperties().containsKey("textures")) {
                Property tex = profile.getProperties().get("textures").iterator().next();
                sb.append('|').append(tex.getValue()).append('|').append(tex.getSignature());
            }

            byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            deflater.setInput(data);
            deflater.finish();

            byte[] output = new byte[data.length];
            int compressedSize = deflater.deflate(output);
            deflater.end();

            byte[] result = new byte[compressedSize];
            System.arraycopy(output, 0, result, 0, compressedSize);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private void flushQueue() {
        if (updateQueue.isEmpty()) return;

        CompressedUpdate[] batch = new CompressedUpdate[Math.min(updateQueue.size(), 20)];
        int count = updateQueue.drainTo(java.util.Arrays.asList(batch).subList(0, updateQueue.size()));

        for (int i = 0; i < count; i++) {
            redis.publish(REDIS_CHANNEL, java.util.Base64.getEncoder().encodeToString(batch[i].data));
        }
    }

    private void processCompressedMessage(String base64) {
        try {
            byte[] compressed = java.util.Base64.getDecoder().decode(base64);
            byte[] decompressed = decompress(compressed);
            String[] parts = new String(decompressed, StandardCharsets.UTF_8).split("\\|");

            if (parts[0].equals(lobbyId)) return;

            UUID playerId = UUID.fromString(parts[1]);
            String playerName = parts[2];
            String[] coords = parts[3].split(",");
            Location loc = new Location(Bukkit.getWorld("world"),
                    Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1]),
                    Double.parseDouble(coords[2]),
                    Float.parseFloat(coords[3]),
                    Float.parseFloat(coords[4]));

            boolean sneaking = parts[4].charAt(0) == '1';
            boolean sprinting = parts[4].charAt(1) == '1';
            int slot = Integer.parseInt(parts[5]);

            String skinTexture = parts.length > 6 ? parts[6] : null;
            String skinSignature = parts.length > 7 ? parts[7] : null;

            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                    handleEntityUpdate(playerId, playerName, parts[0], loc, sneaking, sprinting, slot, skinTexture, skinSignature));

        } catch (Exception ignored) {}
    }

    private byte[] decompress(byte[] compressed) throws Exception {
        java.util.zip.Inflater inflater = new java.util.zip.Inflater();
        inflater.setInput(compressed);
        byte[] result = new byte[compressed.length * 4];
        int size = inflater.inflate(result);
        inflater.end();
        byte[] output = new byte[size];
        System.arraycopy(result, 0, output, 0, size);
        return output;
    }

    private void handleEntityUpdate(UUID id, String name, String lobby, Location loc,
                                    boolean sneak, boolean sprint, int slot,
                                    String tex, String sig) {
        RemoteEntity entity = entities.get(id);

        if (entity == null) {
            entity = new RemoteEntity(id, name, lobby, loc);
            if (tex != null && sig != null) {
                entity.setSkin(tex, sig);
            }
            entities.put(id, entity);
            RemoteEntity finalEntity = entity;
            Bukkit.getOnlinePlayers().forEach(finalEntity::spawn);
        } else {
            entity.updateState(loc, sneak, sprint, slot);
        }
    }

    public void handlePlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () ->
                entities.values().forEach(e -> e.spawn(player)), 20L);
    }

    public void handlePlayerQuit(Player player) {
        stateCache.remove(player.getUniqueId());
    }

    private record CompressedUpdate(UUID playerId, byte[] data) {}

    private static class CachedState {
        final Location location;

        CachedState(Location loc) {
            this.location = loc.clone();
        }
    }

    @Getter
    private static class RemoteEntity {
        private final UUID id;
        private final String name;
        private final String lobby;
        private final EntityPlayer nms;
        private final GameProfile profile;
        private Location location;
        private long lastUpdate;

        RemoteEntity(UUID id, String name, String lobby, Location loc) {
            this.id = id;
            this.name = name;
            this.lobby = lobby;
            this.location = loc;
            this.lastUpdate = System.currentTimeMillis();
            this.profile = new GameProfile(id, "§7[" + lobby + "] §f" + name);

            MinecraftServer server = MinecraftServer.getServer();
            WorldServer world = server.getWorldServer(0);
            this.nms = new EntityPlayer(server, world, profile, new PlayerInteractManager(world));
            nms.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }

        void setSkin(String texture, String signature) {
            profile.getProperties().put("textures", new Property("textures", texture, signature));
        }

        void spawn(Player viewer) {
            PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
            conn.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, nms));
            conn.sendPacket(new PacketPlayOutNamedEntitySpawn(nms));
            conn.sendPacket(new PacketPlayOutEntityHeadRotation(nms, (byte)((location.getYaw() * 256.0F) / 360.0F)));
        }

        void updateState(Location newLoc, boolean sneak, boolean sprint, int slot) {
            this.lastUpdate = System.currentTimeMillis();

            double dx = (newLoc.getX() * 32 - location.getX() * 32) * 128;
            double dy = (newLoc.getY() * 32 - location.getY() * 32) * 128;
            double dz = (newLoc.getZ() * 32 - location.getZ() * 32) * 128;
            byte yaw = (byte)((newLoc.getYaw() * 256.0F) / 360.0F);
            byte pitch = (byte)((newLoc.getPitch() * 256.0F) / 360.0F);

            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerConnection conn = ((CraftPlayer) p).getHandle().playerConnection;

                if (Math.abs(dx) > 127 || Math.abs(dy) > 127 || Math.abs(dz) > 127) {
                    conn.sendPacket(new PacketPlayOutEntityTeleport(nms.getId(),
                            MathHelper.floor(newLoc.getX() * 32.0D),
                            MathHelper.floor(newLoc.getY() * 32.0D),
                            MathHelper.floor(newLoc.getZ() * 32.0D),
                            yaw, pitch, nms.onGround));
                } else {
                    conn.sendPacket(new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                            nms.getId(), (byte)dx, (byte)dy, (byte)dz, yaw, pitch, nms.onGround));
                }

                conn.sendPacket(new PacketPlayOutEntityHeadRotation(nms, yaw));
            }

            updateMetadata(sneak, sprint);
            this.location = newLoc;
            nms.inventory.itemInHandIndex = slot;
        }

        private void updateMetadata(boolean sneak, boolean sprint) {
            DataWatcher watcher = nms.getDataWatcher();
            byte flags = watcher.getByte(0);
            flags = sneak ? (byte)(flags | 0x02) : (byte)(flags & ~0x02);
            flags = sprint ? (byte)(flags | 0x08) : (byte)(flags & ~0x08);
            watcher.watch(0, flags);

            PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(nms.getId(), watcher, true);
            Bukkit.getOnlinePlayers().forEach(p -> ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packet));
        }

        void destroy() {
            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(nms.getId());
            PacketPlayOutPlayerInfo remove = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nms);

            Bukkit.getOnlinePlayers().forEach(p -> {
                PlayerConnection conn = ((CraftPlayer)p).getHandle().playerConnection;
                conn.sendPacket(destroy);
                conn.sendPacket(remove);
            });
        }
    }
}
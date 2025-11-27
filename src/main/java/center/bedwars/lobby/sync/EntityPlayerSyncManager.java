package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.sync.serialization.EntitySerializer;
import center.bedwars.lobby.sync.serialization.EntitySerializer.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public final class EntityPlayerSyncManager extends Manager {

    private static final String REDIS_CHANNEL = "bwl:ep";
    private static final int UPDATE_MS = 50;
    private static final double MOVE_THRESHOLD = 0.05D;
    private static final double TELEPORT_THRESHOLD = 8.0D;
    private static final float ROT_THRESHOLD = 5.0F;
    private static final int TIMEOUT_MS = 120_000;
    private static final int BATCH_SIZE = 100;

    private final Map<UUID, RemoteEntity> entities = new ConcurrentHashMap<>();
    private final Map<UUID, CachedState> cache = new ConcurrentHashMap<>();
    private final BlockingQueue<EntityPacket> queue = new LinkedBlockingQueue<>(1000);
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
    private RedisDatabase redis;
    private byte lobbyId;
    private volatile boolean running = false;

    @Override
    protected void onLoad() {
        this.lobbyId = getLobbyIdAsByte(SettingsConfiguration.LOBBY_ID);
        redis = Lobby.getManagerStorage().getManager(DatabaseManager.class).getRedis();
        running = true;
        redis.subscribe(REDIS_CHANNEL, this::onRedis);
        exec.scheduleAtFixedRate(this::broadcastLocal, 0, UPDATE_MS, TimeUnit.MILLISECONDS);
        exec.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void onUnload() {
        running = false;
        exec.shutdownNow();
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            entities.values().forEach(RemoteEntity::destroy);
            entities.clear();
        });
    }

    private void broadcastLocal() {
        if (!running) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            CachedState c = cache.get(p.getUniqueId());
            Location loc = p.getLocation();

            if (c == null) {
                spawnEntity(p, loc);
                continue;
            }

            if (c.needsSpawn) {
                spawnEntity(p, loc);
                c.needsSpawn = false;
                continue;
            }

            if (c.force) {
                handleForced(p, c, loc);
                c.force = false;
                continue;
            }

            double distSq = c.loc.distanceSquared(loc);
            boolean rotChanged = Math.abs(c.loc.getYaw() - loc.getYaw()) > ROT_THRESHOLD ||
                    Math.abs(c.loc.getPitch() - loc.getPitch()) > ROT_THRESHOLD;

            if (distSq > TELEPORT_THRESHOLD * TELEPORT_THRESHOLD) {
                teleportEntity(p, loc);
            } else if (distSq > MOVE_THRESHOLD * MOVE_THRESHOLD || rotChanged) {
                moveEntity(p, c.loc, loc);
            }

            if (c.sneak != p.isSneaking()) {
                sneakEntity(p, p.isSneaking());
                c.sneak = p.isSneaking();
            }

            if (c.sprint != p.isSprinting()) {
                sprintEntity(p, p.isSprinting());
                c.sprint = p.isSprinting();
            }

            if (c.swing > 0) {
                swingEntity(p, c.swing == 1);
                c.swing = 0;
            }

            c.loc = loc.clone();
        }

        flushQueue();
    }

    private void spawnEntity(Player p, Location loc) {
        CraftPlayer craftPlayer = (CraftPlayer) p;
        GameProfile profile = craftPlayer.getProfile();

        String texture = "";
        String signature = "";

        if (profile.getProperties().containsKey("textures")) {
            Property textureProp = profile.getProperties().get("textures").iterator().next();
            texture = textureProp.getValue();
            signature = textureProp.getSignature() != null ? textureProp.getSignature() : "";
        }

        SpawnEntityPacket packet = new SpawnEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.name = p.getName();
        packet.texture = texture;
        packet.signature = signature;
        packet.x = loc.getX();
        packet.y = loc.getY();
        packet.z = loc.getZ();
        packet.yaw = loc.getYaw();
        packet.pitch = loc.getPitch();
        packet.sneak = p.isSneaking();
        packet.sprint = p.isSprinting();
        packet.slot = (byte) p.getInventory().getHeldItemSlot();

        queue.offer(packet);
        cache.put(p.getUniqueId(), new CachedState(loc, p.isSneaking(), p.isSprinting()));
    }

    private void moveEntity(Player p, Location from, Location to) {
        double dx = (to.getX() - from.getX()) * 32.0;
        double dy = (to.getY() - from.getY()) * 32.0;
        double dz = (to.getZ() - from.getZ()) * 32.0;

        if (Math.abs(dx) > 127 || Math.abs(dy) > 127 || Math.abs(dz) > 127) {
            teleportEntity(p, to);
            return;
        }

        MoveEntityPacket packet = new MoveEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.dx = (byte) dx;
        packet.dy = (byte) dy;
        packet.dz = (byte) dz;
        packet.yaw = (byte) ((to.getYaw() * 256f) / 360f);
        packet.pitch = (byte) ((to.getPitch() * 256f) / 360f);

        queue.offer(packet);
    }

    private void teleportEntity(Player p, Location loc) {
        TeleportEntityPacket packet = new TeleportEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.x = loc.getX();
        packet.y = loc.getY();
        packet.z = loc.getZ();
        packet.yaw = loc.getYaw();
        packet.pitch = loc.getPitch();
        packet.worldId = 0;

        queue.offer(packet);
    }

    private void sneakEntity(Player p, boolean sneaking) {
        SneakEntityPacket packet = new SneakEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.sneaking = sneaking;

        queue.offer(packet);
    }

    private void sprintEntity(Player p, boolean sprinting) {
        SprintEntityPacket packet = new SprintEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.sprinting = sprinting;

        queue.offer(packet);
    }

    private void swingEntity(Player p, boolean mainHand) {
        SwingEntityPacket packet = new SwingEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.mainHand = mainHand;

        queue.offer(packet);
    }

    private void slotEntity(Player p, byte slot) {
        SlotEntityPacket packet = new SlotEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        packet.slot = slot;

        queue.offer(packet);
    }

    private void handleForced(Player p, CachedState c, Location loc) {
        if (c.swing > 0) {
            swingEntity(p, c.swing == 1);
            c.swing = 0;
        }

        if (c.sneak != p.isSneaking()) {
            sneakEntity(p, p.isSneaking());
            c.sneak = p.isSneaking();
        }

        if (c.sprint != p.isSprinting()) {
            sprintEntity(p, p.isSprinting());
            c.sprint = p.isSprinting();
        }
    }

    private void flushQueue() {
        if (queue.isEmpty()) return;

        List<EntityPacket> batch = new ArrayList<>(BATCH_SIZE);
        EntityPacket packet;

        while ((packet = queue.poll()) != null && batch.size() < BATCH_SIZE) {
            batch.add(packet);
        }

        try {
            byte[] serialized = EntitySerializer.serializeBatch(batch);
            redis.publish(REDIS_CHANNEL, serialized);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onRedis(byte[] raw) {
        try {
            List<EntityPacket> batch = EntitySerializer.deserializeBatch(raw);

            for (EntityPacket packet : batch) {
                if (lobbyId == packet.getLobbyId()) continue;

                Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> handlePacket(packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePacket(EntityPacket packet) {
        UUID id = packet.getPlayerId();
        RemoteEntity re = entities.get(id);

        switch (packet.getType()) {
            case SPAWN:
                SpawnEntityPacket spawn = (SpawnEntityPacket) packet;
                if (re == null) {
                    re = new RemoteEntity(id, spawn.name, spawn.texture, spawn.signature,
                            spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);
                    entities.put(id, re);
                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        re.spawn(viewer);
                    }
                }
                re.updateMetadata(spawn.sneak, spawn.sprint, spawn.slot);
                break;

            case MOVE:
                if (re != null) {
                    MoveEntityPacket move = (MoveEntityPacket) packet;
                    re.move(move.dx, move.dy, move.dz, move.yaw, move.pitch);
                }
                break;

            case TELEPORT:
                if (re != null) {
                    TeleportEntityPacket tp = (TeleportEntityPacket) packet;
                    re.teleport(tp.x, tp.y, tp.z, tp.yaw, tp.pitch);
                }
                break;

            case SNEAK:
                if (re != null) {
                    SneakEntityPacket sneak = (SneakEntityPacket) packet;
                    re.setSneak(sneak.sneaking);
                }
                break;

            case SPRINT:
                if (re != null) {
                    SprintEntityPacket sprint = (SprintEntityPacket) packet;
                    re.setSprint(sprint.sprinting);
                }
                break;

            case SWING:
                if (re != null) {
                    SwingEntityPacket swing = (SwingEntityPacket) packet;
                    re.swing(swing.mainHand);
                }
                break;

            case SLOT:
                if (re != null) {
                    SlotEntityPacket slot = (SlotEntityPacket) packet;
                    re.setSlot(slot.slot);
                }
                break;

            case DESPAWN:
                if (re != null) {
                    re.destroy();
                    entities.remove(id);
                }
                break;
        }
    }

    public void swingArm(Player p, boolean mainHand) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) {
            c.swing = mainHand ? (byte) 1 : (byte) 2;
            c.force = true;
        }
    }

    public void handleAnimation(Player p, int type) {
        swingArm(p, type == 0);
    }

    public void handleSneakChange(Player p, boolean b) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) {
            c.sneak = b;
            c.force = true;
        }
    }

    public void handleSprintChange(Player p, boolean b) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) {
            c.sprint = b;
            c.force = true;
        }
    }

    public void handleHeldSlotChange(Player p, int slot) {
        slotEntity(p, (byte) slot);
    }

    public void handlePlayerJoin(Player p) {
        CachedState state = new CachedState(p.getLocation(), false, false);
        state.needsSpawn = true;
        cache.put(p.getUniqueId(), state);
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () ->
                entities.values().forEach(re -> re.spawn(p)), 15L);
    }

    public void handlePlayerQuit(Player p) {
        cache.remove(p.getUniqueId());

        DespawnEntityPacket packet = new DespawnEntityPacket();
        packet.lobbyId = lobbyId;
        packet.playerId = p.getUniqueId();
        queue.offer(packet);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        entities.entrySet().removeIf(e -> {
            if (now - e.getValue().lastUpdate > TIMEOUT_MS) {
                Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> e.getValue().destroy());
                return true;
            }
            return false;
        });
    }

    private byte getLobbyIdAsByte(String lobbyIdStr) {
        try {
            String[] parts = lobbyIdStr.split("-");
            if (parts.length > 0) {
                return Byte.parseByte(parts[parts.length - 1]);
            }
        } catch (Exception ignored) {}
        return (byte) lobbyIdStr.hashCode();
    }

    private static final class CachedState {
        Location loc;
        boolean sneak, sprint;
        byte swing;
        boolean force;
        boolean needsSpawn;

        CachedState(Location l, boolean s1, boolean s2) {
            this.loc = l.clone();
            this.sneak = s1;
            this.sprint = s2;
            this.swing = 0;
        }
    }

    private static final class RemoteEntity {
        private final EntityPlayer nms;
        private double x, y, z;
        private float yaw, pitch;
        private boolean sneak, sprint;
        private byte slot;
        long lastUpdate = System.currentTimeMillis();

        RemoteEntity(UUID id, String name, String texture, String signature,
                     double x, double y, double z, float yaw, float pitch) {
            GameProfile profile = new GameProfile(id, name);

            if (!texture.isEmpty()) {
                profile.getProperties().put("textures", new Property("textures", texture, signature));
            }

            MinecraftServer srv = MinecraftServer.getServer();
            WorldServer ws = srv.getWorldServer(0);
            this.nms = new EntityPlayer(srv, ws, profile, new PlayerInteractManager(ws));

            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;

            nms.setLocation(x, y, z, yaw, pitch);
        }

        void move(byte dx, byte dy, byte dz, byte yaw, byte pitch) {
            this.lastUpdate = System.currentTimeMillis();

            this.x += dx / 32.0;
            this.y += dy / 32.0;
            this.z += dz / 32.0;
            this.yaw = (yaw * 360f) / 256f;
            this.pitch = (pitch * 360f) / 256f;

            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook pkt =
                    new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                            nms.getId(), dx, dy, dz, yaw, pitch, nms.onGround);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
                conn.sendPacket(pkt);
                conn.sendPacket(new PacketPlayOutEntityHeadRotation(nms, yaw));
            }

            nms.setLocation(this.x, this.y, this.z, this.yaw, this.pitch);
        }

        void teleport(double x, double y, double z, float yaw, float pitch) {
            this.lastUpdate = System.currentTimeMillis();

            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;

            byte yawByte = (byte) ((yaw * 256f) / 360f);
            byte pitchByte = (byte) ((pitch * 256f) / 360f);

            PacketPlayOutEntityTeleport pkt = new PacketPlayOutEntityTeleport(
                    nms.getId(),
                    MathHelper.floor(x * 32),
                    MathHelper.floor(y * 32),
                    MathHelper.floor(z * 32),
                    yawByte, pitchByte, nms.onGround
            );

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
                conn.sendPacket(pkt);
                conn.sendPacket(new PacketPlayOutEntityHeadRotation(nms, yawByte));
            }

            nms.setLocation(x, y, z, yaw, pitch);
        }

        void setSneak(boolean sneak) {
            this.lastUpdate = System.currentTimeMillis();
            this.sneak = sneak;
            updateMetadata(sneak, this.sprint, this.slot);
        }

        void setSprint(boolean sprint) {
            this.lastUpdate = System.currentTimeMillis();
            this.sprint = sprint;
            updateMetadata(this.sneak, sprint, this.slot);
        }

        void setSlot(byte slot) {
            this.lastUpdate = System.currentTimeMillis();
            this.slot = slot;
            nms.inventory.itemInHandIndex = slot;
        }

        void updateMetadata(boolean sneak, boolean sprint, byte slot) {
            this.sneak = sneak;
            this.sprint = sprint;
            this.slot = slot;

            nms.inventory.itemInHandIndex = slot;

            DataWatcher dw = nms.getDataWatcher();
            byte flags = dw.getByte(0);
            flags = sneak ? (byte) (flags | 0x02) : (byte) (flags & ~0x02);
            flags = sprint ? (byte) (flags | 0x08) : (byte) (flags & ~0x08);
            dw.watch(0, flags);

            PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(nms.getId(), dw, true);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(meta);
            }
        }

        void swing(boolean mainHand) {
            this.lastUpdate = System.currentTimeMillis();
            PacketPlayOutAnimation pkt = new PacketPlayOutAnimation(nms, mainHand ? 0 : 3);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(pkt);
            }
        }

        void spawn(Player viewer) {
            PlayerConnection c = ((CraftPlayer) viewer).getHandle().playerConnection;
            c.sendPacket(new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, nms
            ));
            c.sendPacket(new PacketPlayOutNamedEntitySpawn(nms));
            c.sendPacket(new PacketPlayOutEntityHeadRotation(nms, (byte) ((nms.yaw * 256f) / 360f)));

            DataWatcher dw = nms.getDataWatcher();
            PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(nms.getId(), dw, true);
            c.sendPacket(meta);
        }

        void destroy() {
            PacketPlayOutEntityDestroy d = new PacketPlayOutEntityDestroy(nms.getId());
            PacketPlayOutPlayerInfo r = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nms
            );
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                PlayerConnection c = ((CraftPlayer) viewer).getHandle().playerConnection;
                c.sendPacket(d);
                c.sendPacket(r);
            }
        }
    }
}
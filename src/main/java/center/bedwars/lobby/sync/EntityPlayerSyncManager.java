package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

public final class EntityPlayerSyncManager extends Manager {

    private static final String REDIS_CHANNEL = "bwl:ep";
    private static final int UPDATE_MS = 50;
    private static final double POS_THRESHOLD = 0.05D;
    private static final float ROT_THRESHOLD = 5.0F;
    private static final int TIMEOUT_MS = 120_000;

    private final Map<UUID, RemoteEntity> entities = new ConcurrentHashMap<>();
    private final Map<UUID, CachedState> cache = new ConcurrentHashMap<>();
    private final BlockingQueue<ByteBuf> queue = new LinkedBlockingQueue<>(500);
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
    private RedisDatabase redis;
    private String lobbyId;
    private volatile boolean running = false;

    @Override protected void onLoad() {
        lobbyId = SettingsConfiguration.LOBBY_ID;
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
            if (c != null && !c.force && !significant(c, loc, p)) continue;

            ByteBuf buf = Unpooled.buffer();
            writeUTF(buf, lobbyId);
            writeUTF(buf, p.getUniqueId().toString());
            writeUTF(buf, p.getName());
            buf.writeDouble(loc.getX());
            buf.writeDouble(loc.getY());
            buf.writeDouble(loc.getZ());
            buf.writeFloat(loc.getYaw());
            buf.writeFloat(loc.getPitch());
            buf.writeBoolean(p.isSneaking());
            buf.writeBoolean(p.isSprinting());
            buf.writeInt(p.getInventory().getHeldItemSlot());
            buf.writeByte(c == null ? 0 : c.swing);
            queue.offer(buf);
            cache.put(p.getUniqueId(), new CachedState(loc, p.isSneaking(), p.isSprinting(), (byte) 0));
        }
        if (!queue.isEmpty()) {
            List<byte[]> batch = new ArrayList<>();
            ByteBuf b;
            while ((b = queue.poll()) != null && batch.size() < 50) {
                byte[] arr = new byte[b.readableBytes()];
                b.readBytes(arr);
                batch.add(arr);
            }
            ByteBuf out = Unpooled.buffer();
            out.writeInt(batch.size());
            for (byte[] bytes : batch) {
                out.writeInt(bytes.length);
                out.writeBytes(bytes);
            }
            redis.publish(REDIS_CHANNEL, out.array());
        }
    }

    private void onRedis(byte[] raw) {
        ByteBuf buf = Unpooled.wrappedBuffer(raw);
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            int len = buf.readInt();
            byte[] data = new byte[len];
            buf.readBytes(data);
            ByteBuf b = Unpooled.wrappedBuffer(data);
            String lobby = readUTF(b);
            if (lobbyId.equals(lobby)) continue;
            UUID id = UUID.fromString(readUTF(b));
            String name = readUTF(b);
            Location loc = new Location(Bukkit.getWorld("world"), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat());
            boolean sneak = b.readBoolean();
            boolean sprint = b.readBoolean();
            int slot = b.readInt();
            byte swing = b.readByte();
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> handleUpdate(id, name, loc, sneak, sprint, slot, swing));
        }
    }

    private void handleUpdate(UUID id, String name, Location loc, boolean sneak, boolean sprint, int slot, byte swing) {
        RemoteEntity re = entities.computeIfAbsent(id, k -> new RemoteEntity(id, name, loc));
        re.update(loc, sneak, sprint, slot);
        if (swing > 0) re.swing(swing == 1);
    }

    public void swingArm(Player p, boolean mainHand) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) { c.swing = mainHand ? (byte) 1 : (byte) 2; c.force = true; }
    }

    public void handleAnimation(Player p, int type) {
        swingArm(p, type == 0);
    }

    public void handleSneakChange(Player p, boolean b) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) { c.sneak = b; c.force = true; }
    }

    public void handleSprintChange(Player p, boolean b) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) { c.sprint = b; c.force = true; }
    }

    public void handleHeldSlotChange(Player p, int slot) {
        CachedState c = cache.get(p.getUniqueId());
        if (c != null) c.force = true;
    }

    public void handlePlayerJoin(Player p) {
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            entities.values().forEach(re -> re.spawn(p));
        }, 10L);
    }

    public void handlePlayerQuit(Player p) {
        cache.remove(p.getUniqueId());
    }

    private boolean significant(CachedState c, Location now, Player p) {
        if (c.sneak != p.isSneaking() || c.sprint != p.isSprinting()) return true;
        if (!c.loc.getWorld().equals(now.getWorld())) return true;
        if (c.loc.distanceSquared(now) > POS_THRESHOLD * POS_THRESHOLD) return true;
        if (Math.abs(c.loc.getYaw() - now.getYaw()) > ROT_THRESHOLD) return true;
        if (Math.abs(c.loc.getPitch() - now.getPitch()) > ROT_THRESHOLD) return true;
        return false;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        entities.entrySet().removeIf(e -> {
            if (now - e.getValue().lastUpdate > TIMEOUT_MS) {
                e.getValue().destroy(); return true;
            }
            return false;
        });
    }

    private static void writeUTF(ByteBuf buf, String s) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(b.length);
        buf.writeBytes(b);
    }

    private static String readUTF(ByteBuf buf) {
        int len = buf.readShort();
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static final class CachedState {
        final Location loc;
        boolean sneak, sprint;
        byte swing;
        boolean force;
        CachedState(Location l, boolean s1, boolean s2, byte sw) {
            this.loc = l.clone(); this.sneak = s1; this.sprint = s2; this.swing = sw;
        }
    }

    private static final class RemoteEntity {
        private final UUID id;
        private final String name;
        private final GameProfile profile;
        private final EntityPlayer nms;
        long lastUpdate = System.currentTimeMillis();

        RemoteEntity(UUID id, String name, Location loc) {
            this.id = id;
            this.name = name;
            this.profile = new GameProfile(id, name);
            MinecraftServer srv = MinecraftServer.getServer();
            WorldServer ws = srv.getWorldServer(0);
            this.nms = new EntityPlayer(srv, ws, profile, new PlayerInteractManager(ws));
            nms.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }

        void update(Location loc, boolean sneak, boolean sprint, int slot) {
            this.lastUpdate = System.currentTimeMillis();
            double dx = (loc.getX() * 32 - nms.locX * 32) * 128;
            double dy = (loc.getY() * 32 - nms.locY * 32) * 128;
            double dz = (loc.getZ() * 32 - nms.locZ * 32) * 128;
            byte yaw = (byte) ((loc.getYaw() * 256f) / 360f);
            byte pitch = (byte) ((loc.getPitch() * 256f) / 360f);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
                if (Math.abs(dx) > 127 || Math.abs(dy) > 127 || Math.abs(dz) > 127) {
                    conn.sendPacket(new PacketPlayOutEntityTeleport(nms.getId(),
                            MathHelper.floor(loc.getX() * 32), MathHelper.floor(loc.getY() * 32),
                            MathHelper.floor(loc.getZ() * 32), yaw, pitch, nms.onGround));
                } else {
                    conn.sendPacket(new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                            nms.getId(), (byte) dx, (byte) dy, (byte) dz, yaw, pitch, nms.onGround));
                }
                conn.sendPacket(new PacketPlayOutEntityHeadRotation(nms, yaw));
            }
            nms.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            nms.inventory.itemInHandIndex = slot;

            DataWatcher dw = nms.getDataWatcher();
            byte flags = dw.getByte(0);
            flags = sneak ? (byte) (flags | 0x02) : (byte) (flags & ~0x02);
            flags = sprint ? (byte) (flags | 0x08) : (byte) (flags & ~0x08);
            dw.watch(0, flags);
            PacketPlayOutEntityMetadata meta = new PacketPlayOutEntityMetadata(nms.getId(), dw, true);
            for (Player viewer : Bukkit.getOnlinePlayers())
                ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(meta);
        }

        void swing(boolean mainHand) {
            PacketPlayOutAnimation pkt = new PacketPlayOutAnimation(nms, mainHand ? 0 : 3);
            for (Player viewer : Bukkit.getOnlinePlayers())
                ((CraftPlayer) viewer).getHandle().playerConnection.sendPacket(pkt);
        }

        void spawn(Player viewer) {
            PlayerConnection c = ((CraftPlayer) viewer).getHandle().playerConnection;
            c.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, nms));
            c.sendPacket(new PacketPlayOutNamedEntitySpawn(nms));
            c.sendPacket(new PacketPlayOutEntityHeadRotation(nms, (byte) ((nms.yaw * 256f) / 360f)));
        }

        void destroy() {
            PacketPlayOutEntityDestroy d = new PacketPlayOutEntityDestroy(nms.getId());
            PacketPlayOutPlayerInfo r = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nms);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                PlayerConnection c = ((CraftPlayer) viewer).getHandle().playerConnection;
                c.sendPacket(d); c.sendPacket(r);
            }
        }
    }
}
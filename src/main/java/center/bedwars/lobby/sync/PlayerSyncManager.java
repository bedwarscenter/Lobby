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
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

public class PlayerSyncManager extends Manager {

    private static final String REDIS_CHANNEL = "bwl:ps";
    private static final int HEARTBEAT_INTERVAL_SEC = 3;
    private static final int TIMEOUT_MS = 10000;

    private String lobbyId;
    private RedisDatabase redis;
    private final Map<UUID, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = false;

    @Override
    protected void onLoad() {
        lobbyId = SettingsConfiguration.LOBBY_ID;
        redis = Lobby.getManagerStorage().getManager(DatabaseManager.class).getRedis();
        running = true;
        setupSubscription();
        startHeartbeat();
    }

    @Override
    protected void onUnload() {
        running = false;
        executor.shutdownNow();
        remotePlayers.clear();
    }

    private void setupSubscription() {
        redis.subscribe(REDIS_CHANNEL, this::processMessage);
    }

    private void startHeartbeat() {
        executor.scheduleAtFixedRate(() -> {
            if (!running) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                broadcast(player, "H");
            }
            cleanup();
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void processMessage(byte[] raw) {
        ByteBuf buf = Unpooled.wrappedBuffer(raw);
        String lobby = readUTF(buf);
        if (lobbyId.equals(lobby)) return;
        String action = readUTF(buf);
        UUID id = UUID.fromString(readUTF(buf));
        String name = readUTF(buf);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            switch (action) {
                case "J": handleJoin(id, name, lobby); break;
                case "Q": handleQuit(id); break;
                case "H": handleHeartbeat(id, name, lobby); break;
            }
        });
    }

    private void handleJoin(UUID id, String name, String lobby) {
        RemotePlayer rp = new RemotePlayer(id, name, lobby);
        remotePlayers.put(id, rp);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) addToTab(player, rp);
        });
    }

    private void handleQuit(UUID id) {
        remotePlayers.remove(id);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) removeFromTab(player, id);
        });
    }

    private void handleHeartbeat(UUID id, String name, String lobby) {
        RemotePlayer rp = remotePlayers.get(id);
        if (rp == null) handleJoin(id, name, lobby);
        else rp.lastUpdate = System.currentTimeMillis();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, RemotePlayer> e : remotePlayers.entrySet())
            if (now - e.getValue().lastUpdate > TIMEOUT_MS) toRemove.add(e.getKey());
        if (!toRemove.isEmpty()) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                for (UUID id : toRemove) handleQuit(id);
            });
        }
    }

    public void handlePlayerJoin(Player player) {
        broadcast(player, "J");
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (RemotePlayer rp : remotePlayers.values()) addToTab(player, rp);
        }, 5L);
    }

    public void handlePlayerQuit(Player player) {
        broadcast(player, "Q");
    }

    private void broadcast(Player player, String action) {
        CompletableFuture.runAsync(() -> {
            ByteBuf buf = Unpooled.buffer();
            writeUTF(buf, lobbyId);
            writeUTF(buf, action);
            writeUTF(buf, player.getUniqueId().toString());
            writeUTF(buf, player.getName());
            redis.publish(REDIS_CHANNEL, buf.array());
        });
    }

    private void addToTab(Player viewer, RemotePlayer remote) {
        try {
            PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
            GameProfile profile = new GameProfile(remote.id, remote.name);
            EntityPlayer dummy = new EntityPlayer(MinecraftServer.getServer(), MinecraftServer.getServer().getWorldServer(0), profile, new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0)));
            conn.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, dummy));
        } catch (Exception ignored) {}
    }

    private void removeFromTab(Player viewer, UUID id) {
        try {
            PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
            GameProfile profile = new GameProfile(id, "");
            EntityPlayer dummy = new EntityPlayer(MinecraftServer.getServer(), MinecraftServer.getServer().getWorldServer(0), profile, new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0)));
            conn.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, dummy));
        } catch (Exception ignored) {}
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

    private static final class RemotePlayer {
        private final UUID id;
        private final String name;
        private final String lobby;
        private long lastUpdate;
        RemotePlayer(UUID id, String name, String lobby) {
            this.id = id;
            this.name = name;
            this.lobby = lobby;
            this.lastUpdate = System.currentTimeMillis();
        }
    }
}
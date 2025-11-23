package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Getter
public class PlayerSyncManager extends Manager {

    private static final String REDIS_CHANNEL = "bwl:ps";
    private static final int HEARTBEAT_INTERVAL_SEC = 5;
    private static final int TIMEOUT_MS = 15000;

    private final String lobbyId = SettingsConfiguration.LOBBY_ID;
    private RedisDatabase redis;
    private final Map<UUID, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onLoad() {
        this.redis = Lobby.getManagerStorage().getManager(DatabaseManager.class).getRedis();
        setupSubscription();
        startHeartbeat();
    }

    @Override
    protected void onUnload() {
        executor.shutdownNow();
        remotePlayers.clear();
    }

    private void setupSubscription() {
        redis.subscribe(REDIS_CHANNEL, this::processMessage);
    }

    private void startHeartbeat() {
        executor.scheduleAtFixedRate(() -> {
            Bukkit.getOnlinePlayers().forEach(p -> broadcast(p, "H"));
            cleanup();
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    private void processMessage(String msg) {
        try {
            String[] parts = msg.split("\\|");
            if (parts[0].equals(lobbyId)) return;

            String action = parts[1];
            UUID id = UUID.fromString(parts[2]);
            String name = parts[3];

            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                switch (action) {
                    case "J" -> handleJoin(id, name, parts[0]);
                    case "Q" -> handleQuit(id);
                    case "H" -> handleHeartbeat(id, name, parts[0]);
                }
            });
        } catch (Exception ignored) {}
    }

    private void handleJoin(UUID id, String name, String lobby) {
        RemotePlayer rp = new RemotePlayer(id, name, lobby);
        remotePlayers.put(id, rp);
        Bukkit.getOnlinePlayers().forEach(p -> addToTab(p, rp));
    }

    private void handleQuit(UUID id) {
        remotePlayers.remove(id);
        Bukkit.getOnlinePlayers().forEach(p -> removeFromTab(p, id));
    }

    private void handleHeartbeat(UUID id, String name, String lobby) {
        RemotePlayer rp = remotePlayers.get(id);
        if (rp == null) {
            handleJoin(id, name, lobby);
        } else {
            rp.lastUpdate = System.currentTimeMillis();
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        remotePlayers.entrySet().removeIf(e -> now - e.getValue().lastUpdate > TIMEOUT_MS);
    }

    public void handlePlayerJoin(Player player) {
        broadcast(player, "J");
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(),
                () -> remotePlayers.values().forEach(rp -> addToTab(player, rp)), 20L);
    }

    public void handlePlayerQuit(Player player) {
        broadcast(player, "Q");
        Bukkit.getOnlinePlayers().forEach(p -> removeFromTab(p, player.getUniqueId()));
    }

    private void broadcast(Player player, String action) {
        String msg = String.join("|", lobbyId, action, player.getUniqueId().toString(), player.getName());
        redis.publish(REDIS_CHANNEL, msg);
    }

    private void addToTab(Player viewer, RemotePlayer remote) {
        try {
            PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
            GameProfile profile = new GameProfile(remote.id, "§7[" + remote.lobby + "] §f" + remote.name);

            EntityPlayer dummy = new EntityPlayer(
                    MinecraftServer.getServer(),
                    MinecraftServer.getServer().getWorldServer(0),
                    profile,
                    new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0))
            );

            conn.sendPacket(new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, dummy));
        } catch (Exception ignored) {}
    }

    private void removeFromTab(Player viewer, UUID id) {
        try {
            PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
            GameProfile profile = new GameProfile(id, "");

            EntityPlayer dummy = new EntityPlayer(
                    MinecraftServer.getServer(),
                    MinecraftServer.getServer().getWorldServer(0),
                    profile,
                    new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0))
            );

            conn.sendPacket(new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, dummy));
        } catch (Exception ignored) {}
    }

    @Getter
    private static class RemotePlayer {
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
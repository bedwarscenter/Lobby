package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.sync.serialization.KryoSerializer;
import center.bedwars.lobby.sync.serialization.KryoSerializer.PlayerSyncData;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
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
        try {
            PlayerSyncData data = KryoSerializer.deserialize(raw, PlayerSyncData.class);

            if (lobbyId.equals(data.lobbyId)) return;

            UUID id = UUID.fromString(data.uuid);

            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                switch (data.action) {
                    case "J":
                        handleJoin(id, data.name, data.lobbyId, data.texture, data.signature);
                        break;
                    case "Q":
                        handleQuit(id);
                        break;
                    case "H":
                        handleHeartbeat(id, data.name, data.lobbyId, data.texture, data.signature);
                        break;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleJoin(UUID id, String name, String lobby, String texture, String signature) {
        RemotePlayer rp = new RemotePlayer(id, name, lobby, texture, signature);
        remotePlayers.put(id, rp);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                addToTab(player, rp);
            }
        });
    }

    private void handleQuit(UUID id) {
        remotePlayers.remove(id);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeFromTab(player, id);
            }
        });
    }

    private void handleHeartbeat(UUID id, String name, String lobby, String texture, String signature) {
        RemotePlayer rp = remotePlayers.get(id);
        if (rp == null) {
            handleJoin(id, name, lobby, texture, signature);
        } else {
            rp.lastUpdate = System.currentTimeMillis();
        }
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
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                for (UUID id : toRemove) {
                    handleQuit(id);
                }
            });
        }
    }

    public void handlePlayerJoin(Player player) {
        broadcast(player, "J");
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (RemotePlayer rp : remotePlayers.values()) {
                addToTab(player, rp);
            }
        }, 10L);
    }

    public void handlePlayerQuit(Player player) {
        broadcast(player, "Q");
    }

    private void broadcast(Player player, String action) {
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

                PlayerSyncData data = new PlayerSyncData(
                        lobbyId,
                        action,
                        player.getUniqueId().toString(),
                        player.getName(),
                        texture,
                        signature
                );

                byte[] serialized = KryoSerializer.serialize(data);
                redis.publish(REDIS_CHANNEL, serialized);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void addToTab(Player viewer, RemotePlayer remote) {
        try {
            PlayerConnection conn = ((CraftPlayer) viewer).getHandle().playerConnection;
            GameProfile profile = new GameProfile(remote.id, remote.name);

            if (!remote.texture.isEmpty()) {
                profile.getProperties().put("textures", new Property("textures", remote.texture, remote.signature));
            }

            EntityPlayer dummy = new EntityPlayer(
                    MinecraftServer.getServer(),
                    MinecraftServer.getServer().getWorldServer(0),
                    profile,
                    new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0))
            );

            conn.sendPacket(new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER,
                    dummy
            ));
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
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER,
                    dummy
            ));
        } catch (Exception ignored) {}
    }

    private static final class RemotePlayer {
        private final UUID id;
        private final String name;
        private final String lobby;
        private final String texture;
        private final String signature;
        private long lastUpdate;

        RemotePlayer(UUID id, String name, String lobby, String texture, String signature) {
            this.id = id;
            this.name = name;
            this.lobby = lobby;
            this.texture = texture;
            this.signature = signature;
            this.lastUpdate = System.currentTimeMillis();
        }
    }
}
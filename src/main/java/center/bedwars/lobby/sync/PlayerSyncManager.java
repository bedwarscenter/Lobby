package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class PlayerSyncManager extends Manager {

    private static final String REDIS_PLAYER_CHANNEL = "bedwars:lobby:players";
    private static final Gson GSON = new Gson();

    private final String lobbyId = SettingsConfiguration.LOBBY_ID;
    private RedisDatabase redis;
    private final Map<UUID, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Override
    protected void onLoad() {
        DatabaseManager dbManager = Lobby.getManagerStorage().getManager(DatabaseManager.class);
        this.redis = dbManager.getRedis();

        setupRedisSubscription();
        startHeartbeat();
    }

    @Override
    protected void onUnload() {
        executor.shutdown();
        remotePlayers.clear();
    }

    private void setupRedisSubscription() {
        redis.subscribe(REDIS_PLAYER_CHANNEL, message -> {
            try {
                JsonObject json = GSON.fromJson(message, JsonObject.class);
                String sourceLobby = json.get("lobbyId").getAsString();

                if (sourceLobby.equals(lobbyId)) {
                    return;
                }

                String action = json.get("action").getAsString();
                UUID playerId = UUID.fromString(json.get("playerId").getAsString());
                String playerName = json.get("playerName").getAsString();

                switch (action) {
                    case "JOIN":
                        handleRemoteJoin(playerId, playerName, sourceLobby);
                        break;
                    case "QUIT":
                        handleRemoteQuit(playerId);
                        break;
                    case "HEARTBEAT":
                        handleRemoteHeartbeat(playerId, playerName, sourceLobby);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startHeartbeat() {
        executor.scheduleAtFixedRate(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                broadcastPlayerUpdate(player, "HEARTBEAT");
            }

            remotePlayers.entrySet().removeIf(entry -> {
                RemotePlayer rp = entry.getValue();
                return System.currentTimeMillis() - rp.lastUpdate > 15000;
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void handlePlayerJoin(Player player) {
        broadcastPlayerUpdate(player, "JOIN");

        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (RemotePlayer rp : remotePlayers.values()) {
                addToTabList(player, rp);
            }
        }, 20L);
    }

    public void handlePlayerQuit(Player player) {
        broadcastPlayerUpdate(player, "QUIT");

        for (Player online : Bukkit.getOnlinePlayers()) {
            removeFromTabList(online, player.getUniqueId());
        }
    }

    private void handleRemoteJoin(UUID playerId, String playerName, String sourceLobby) {
        RemotePlayer rp = new RemotePlayer(playerId, playerName, sourceLobby);
        remotePlayers.put(playerId, rp);

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                addToTabList(online, rp);
            }
        });
    }

    private void handleRemoteQuit(UUID playerId) {
        remotePlayers.remove(playerId);

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                removeFromTabList(online, playerId);
            }
        });
    }

    private void handleRemoteHeartbeat(UUID playerId, String playerName, String sourceLobby) {
        RemotePlayer rp = remotePlayers.get(playerId);
        if (rp == null) {
            handleRemoteJoin(playerId, playerName, sourceLobby);
        } else {
            rp.lastUpdate = System.currentTimeMillis();
        }
    }

    private void broadcastPlayerUpdate(Player player, String action) {
        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", action);
        json.addProperty("playerId", player.getUniqueId().toString());
        json.addProperty("playerName", player.getName());
        json.addProperty("timestamp", System.currentTimeMillis());

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    private void addToTabList(Player viewer, RemotePlayer remote) {
        try {
            EntityPlayer nmsPlayer = ((CraftPlayer) viewer).getHandle();
            PlayerConnection connection = nmsPlayer.playerConnection;

            GameProfile profile = new GameProfile(remote.playerId, "§7[" + remote.lobbyId + "] §f" + remote.playerName);

            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER,
                    new EntityPlayer(
                            MinecraftServer.getServer(),
                            MinecraftServer.getServer().getWorldServer(0),
                            profile,
                            new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0))
                    )
            );

            connection.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeFromTabList(Player viewer, UUID playerId) {
        try {
            EntityPlayer nmsPlayer = ((CraftPlayer) viewer).getHandle();
            PlayerConnection connection = nmsPlayer.playerConnection;

            GameProfile profile = new GameProfile(playerId, "");

            PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER,
                    new EntityPlayer(
                            MinecraftServer.getServer(),
                            MinecraftServer.getServer().getWorldServer(0),
                            profile,
                            new PlayerInteractManager(MinecraftServer.getServer().getWorldServer(0))
                    )
            );

            connection.sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Getter
    private static class RemotePlayer {
        private final UUID playerId;
        private final String playerName;
        private final String lobbyId;
        private long lastUpdate;

        public RemotePlayer(UUID playerId, String playerName, String lobbyId) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.lobbyId = lobbyId;
            this.lastUpdate = System.currentTimeMillis();
        }
    }
}
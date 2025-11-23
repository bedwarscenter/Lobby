package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class EntityPlayerSyncManager extends Manager {

    private static final String REDIS_PLAYER_CHANNEL = "bedwars:lobby:entity_players";
    private static final Gson GSON = new Gson();

    private final String lobbyId = SettingsConfiguration.LOBBY_ID;
    private RedisDatabase redis;
    private final Map<UUID, RemotePlayerEntity> remoteEntities = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    @Override
    protected void onLoad() {
        DatabaseManager dbManager = Lobby.getManagerStorage().getManager(DatabaseManager.class);
        this.redis = dbManager.getRedis();

        setupRedisSubscription();
        startBroadcast();
        startCleanup();
    }

    @Override
    protected void onUnload() {
        executor.shutdown();
        remoteEntities.values().forEach(RemotePlayerEntity::destroy);
        remoteEntities.clear();
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

                switch (action) {
                    case "UPDATE":
                        handlePlayerUpdate(json);
                        break;
                    case "QUIT":
                        handlePlayerQuit(json);
                        break;
                    case "ANIMATION":
                        handlePlayerAnimation(json);
                        break;
                    case "SNEAK":
                        handleSneakUpdate(json);
                        break;
                    case "SPRINT":
                        handleSprintUpdate(json);
                        break;
                    case "SLOT":
                        handleSlotUpdate(json);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startBroadcast() {
        executor.scheduleAtFixedRate(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                broadcastPlayerUpdate(player);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void startCleanup() {
        executor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            remoteEntities.entrySet().removeIf(entry -> {
                RemotePlayerEntity entity = entry.getValue();
                if (now - entity.lastUpdate > 3000) {
                    entity.destroy();
                    return true;
                }
                return false;
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void handlePlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (RemotePlayerEntity entity : remoteEntities.values()) {
                entity.spawn(player);
            }
        }, 20L);
    }

    public void handlePlayerQuit(Player player) {
        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", "QUIT");
        json.addProperty("playerId", player.getUniqueId().toString());

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    public void handleAnimation(Player player, int animationId) {
        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", "ANIMATION");
        json.addProperty("playerId", player.getUniqueId().toString());
        json.addProperty("animationId", animationId);

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    public void handleSneakChange(Player player, boolean sneaking) {
        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", "SNEAK");
        json.addProperty("playerId", player.getUniqueId().toString());
        json.addProperty("sneaking", sneaking);

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    public void handleSprintChange(Player player, boolean sprinting) {
        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", "SPRINT");
        json.addProperty("playerId", player.getUniqueId().toString());
        json.addProperty("sprinting", sprinting);

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    public void handleHeldSlotChange(Player player, int slot) {
        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", "SLOT");
        json.addProperty("playerId", player.getUniqueId().toString());
        json.addProperty("slot", slot);

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    private void broadcastPlayerUpdate(Player player) {
        Location loc = player.getLocation();

        JsonObject json = new JsonObject();
        json.addProperty("lobbyId", lobbyId);
        json.addProperty("action", "UPDATE");
        json.addProperty("playerId", player.getUniqueId().toString());
        json.addProperty("playerName", player.getName());
        json.addProperty("x", loc.getX());
        json.addProperty("y", loc.getY());
        json.addProperty("z", loc.getZ());
        json.addProperty("yaw", loc.getYaw());
        json.addProperty("pitch", loc.getPitch());
        json.addProperty("onGround", player.isOnGround());
        json.addProperty("heldSlot", player.getInventory().getHeldItemSlot());

        ItemStack hand = player.getItemInHand();
        if (hand != null && hand.getType() != org.bukkit.Material.AIR) {
            json.addProperty("itemInHand", serializeItem(hand));
        }

        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        GameProfile profile = nmsPlayer.getProfile();
        if (profile.getProperties().containsKey("textures")) {
            Property texture = profile.getProperties().get("textures").iterator().next();
            json.addProperty("skinTexture", texture.getValue());
            json.addProperty("skinSignature", texture.getSignature());
        }

        redis.publish(REDIS_PLAYER_CHANNEL, GSON.toJson(json));
    }

    private void handlePlayerUpdate(JsonObject json) {
        UUID playerId = UUID.fromString(json.get("playerId").getAsString());
        String playerName = json.get("playerName").getAsString();
        String sourceLobby = json.get("lobbyId").getAsString();

        Location loc = new Location(
                Bukkit.getWorld("world"),
                json.get("x").getAsDouble(),
                json.get("y").getAsDouble(),
                json.get("z").getAsDouble(),
                json.get("yaw").getAsFloat(),
                json.get("pitch").getAsFloat()
        );

        RemotePlayerEntity entity = remoteEntities.get(playerId);

        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            if (entity == null) {
                RemotePlayerEntity newEntity = new RemotePlayerEntity(
                        playerId,
                        playerName,
                        sourceLobby,
                        loc
                );

                if (json.has("skinTexture") && json.has("skinSignature")) {
                    newEntity.setSkin(
                            json.get("skinTexture").getAsString(),
                            json.get("skinSignature").getAsString()
                    );
                }

                remoteEntities.put(playerId, newEntity);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    newEntity.spawn(online);
                }
            } else {
                entity.updateLocation(loc);

                if (json.has("itemInHand")) {
                    entity.updateHeldItem(deserializeItem(json.get("itemInHand").getAsString()));
                }

                if (json.has("heldSlot")) {
                    entity.updateHeldSlot(json.get("heldSlot").getAsInt());
                }
            }
        });
    }

    private void handlePlayerQuit(JsonObject json) {
        UUID playerId = UUID.fromString(json.get("playerId").getAsString());
        RemotePlayerEntity entity = remoteEntities.remove(playerId);

        if (entity != null) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), entity::destroy);
        }
    }

    private void handlePlayerAnimation(JsonObject json) {
        UUID playerId = UUID.fromString(json.get("playerId").getAsString());
        int animationId = json.get("animationId").getAsInt();
        RemotePlayerEntity entity = remoteEntities.get(playerId);

        if (entity != null) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                    entity.playAnimation(animationId)
            );
        }
    }

    private void handleSneakUpdate(JsonObject json) {
        UUID playerId = UUID.fromString(json.get("playerId").getAsString());
        boolean sneaking = json.get("sneaking").getAsBoolean();
        RemotePlayerEntity entity = remoteEntities.get(playerId);

        if (entity != null) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                    entity.setSneaking(sneaking)
            );
        }
    }

    private void handleSprintUpdate(JsonObject json) {
        UUID playerId = UUID.fromString(json.get("playerId").getAsString());
        boolean sprinting = json.get("sprinting").getAsBoolean();
        RemotePlayerEntity entity = remoteEntities.get(playerId);

        if (entity != null) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                    entity.setSprinting(sprinting)
            );
        }
    }

    private void handleSlotUpdate(JsonObject json) {
        UUID playerId = UUID.fromString(json.get("playerId").getAsString());
        int slot = json.get("slot").getAsInt();
        RemotePlayerEntity entity = remoteEntities.get(playerId);

        if (entity != null) {
            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () ->
                    entity.updateHeldSlot(slot)
            );
        }
    }

    private String serializeItem(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = new NBTTagCompound();
        nms.save(tag);
        return tag.toString();
    }

    private net.minecraft.server.v1_8_R3.ItemStack deserializeItem(String data) {
        try {
            NBTTagCompound tag = MojangsonParser.parse(data);
            return net.minecraft.server.v1_8_R3.ItemStack.createStack(tag);
        } catch (Exception e) {
            return null;
        }
    }

    @Getter
    private static class RemotePlayerEntity {
        private final UUID playerId;
        private final String playerName;
        private final String lobbyId;
        private final EntityPlayer nmsEntity;
        private final GameProfile profile;
        private Location location;
        private long lastUpdate;

        public RemotePlayerEntity(UUID playerId, String playerName, String lobbyId, Location location) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.lobbyId = lobbyId;
            this.location = location;
            this.lastUpdate = System.currentTimeMillis();

            this.profile = new GameProfile(playerId, "§7[" + lobbyId + "] §f" + playerName);

            MinecraftServer server = MinecraftServer.getServer();
            WorldServer world = server.getWorldServer(0);

            this.nmsEntity = new EntityPlayer(
                    server,
                    world,
                    profile,
                    new PlayerInteractManager(world)
            );

            nmsEntity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        }

        public void setSkin(String texture, String signature) {
            profile.getProperties().put("textures", new Property("textures", texture, signature));
        }

        public void spawn(Player viewer) {
            EntityPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
            PlayerConnection connection = nmsViewer.playerConnection;

            PacketPlayOutPlayerInfo infoPacket = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER,
                    nmsEntity
            );
            connection.sendPacket(infoPacket);

            PacketPlayOutNamedEntitySpawn spawnPacket = new PacketPlayOutNamedEntitySpawn(nmsEntity);
            connection.sendPacket(spawnPacket);

            PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(
                    nmsEntity,
                    (byte) ((location.getYaw() * 256.0F) / 360.0F)
            );
            connection.sendPacket(headPacket);
        }

        public void updateLocation(Location newLoc) {
            this.lastUpdate = System.currentTimeMillis();

            double deltaX = (newLoc.getX() * 32 - location.getX() * 32) * 128;
            double deltaY = (newLoc.getY() * 32 - location.getY() * 32) * 128;
            double deltaZ = (newLoc.getZ() * 32 - location.getZ() * 32) * 128;

            byte yaw = (byte) ((newLoc.getYaw() * 256.0F) / 360.0F);
            byte pitch = (byte) ((newLoc.getPitch() * 256.0F) / 360.0F);

            for (Player online : Bukkit.getOnlinePlayers()) {
                EntityPlayer nmsViewer = ((CraftPlayer) online).getHandle();
                PlayerConnection connection = nmsViewer.playerConnection;

                if (Math.abs(deltaX) > 127 || Math.abs(deltaY) > 127 || Math.abs(deltaZ) > 127) {
                    PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                            nmsEntity.getId(),
                            MathHelper.floor(newLoc.getX() * 32.0D),
                            MathHelper.floor(newLoc.getY() * 32.0D),
                            MathHelper.floor(newLoc.getZ() * 32.0D),
                            yaw,
                            pitch,
                            nmsEntity.onGround
                    );
                    connection.sendPacket(teleportPacket);
                } else {
                    PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook movePacket =
                            new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                                    nmsEntity.getId(),
                                    (byte) deltaX,
                                    (byte) deltaY,
                                    (byte) deltaZ,
                                    yaw,
                                    pitch,
                                    nmsEntity.onGround
                            );
                    connection.sendPacket(movePacket);
                }

                PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation(
                        nmsEntity,
                        yaw
                );
                connection.sendPacket(headPacket);
            }

            this.location = newLoc;
        }

        public void setSneaking(boolean sneaking) {
            this.lastUpdate = System.currentTimeMillis();

            DataWatcher watcher = nmsEntity.getDataWatcher();
            byte flags = watcher.getByte(0);

            if (sneaking) {
                flags |= 0x02;
            } else {
                flags &= ~0x02;
            }

            watcher.watch(0, flags);

            PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(
                    nmsEntity.getId(),
                    watcher,
                    true
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(metaPacket);
            }
        }

        public void setSprinting(boolean sprinting) {
            this.lastUpdate = System.currentTimeMillis();

            DataWatcher watcher = nmsEntity.getDataWatcher();
            byte flags = watcher.getByte(0);

            if (sprinting) {
                flags |= 0x08;
            } else {
                flags &= ~0x08;
            }

            watcher.watch(0, flags);

            PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(
                    nmsEntity.getId(),
                    watcher,
                    true
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(metaPacket);
            }
        }

        public void updateHeldItem(net.minecraft.server.v1_8_R3.ItemStack item) {
            if (item == null) return;

            PacketPlayOutEntityEquipment equipPacket = new PacketPlayOutEntityEquipment(
                    nmsEntity.getId(),
                    0,
                    item
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(equipPacket);
            }
        }

        public void updateHeldSlot(int slot) {
            nmsEntity.inventory.itemInHandIndex = slot;
        }

        public void playAnimation(int animationId) {
            PacketPlayOutAnimation animPacket = new PacketPlayOutAnimation(
                    nmsEntity,
                    animationId
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(animPacket);
            }
        }

        public void destroy() {
            PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(nmsEntity.getId());

            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(destroyPacket);
            }

            PacketPlayOutPlayerInfo removePacket = new PacketPlayOutPlayerInfo(
                    PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER,
                    nmsEntity
            );

            for (Player online : Bukkit.getOnlinePlayers()) {
                ((CraftPlayer) online).getHandle().playerConnection.sendPacket(removePacket);
            }
        }
    }
}
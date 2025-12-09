package center.bedwars.lobby.sync.fake;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class FakePlayer {

    private final UUID uuid;
    private final String name;
    private final GameProfile profile;
    private final int entityId;
    private final Set<UUID> viewers = new HashSet<>();

    @Setter
    private double x, y, z;
    @Setter
    private float yaw, pitch;
    @Setter
    private boolean sneaking, sprinting, flying, onGround;
    @Setter
    private int heldItemSlot;
    @Setter
    private byte sourceLobbyId;
    @Setter
    private long lastUpdate;

    public FakePlayer(UUID uuid, String name, String texture, String signature, int entityId, byte sourceLobbyId) {
        this.uuid = uuid;
        this.name = name;
        this.entityId = entityId;
        this.sourceLobbyId = sourceLobbyId;
        this.lastUpdate = System.currentTimeMillis();

        this.profile = new GameProfile(uuid, name);
        if (texture != null && !texture.isEmpty()) {
            this.profile.getProperties().put("textures", new Property("textures", texture, signature));
        }

        this.x = 0;
        this.y = 64;
        this.z = 0;
        this.yaw = 0;
        this.pitch = 0;
    }

    public void spawn(Player viewer) {
        if (viewer.getUniqueId().equals(uuid))
            return;
        if (!viewers.add(viewer.getUniqueId()))
            return;

        EntityPlayer handle = ((CraftPlayer) viewer).getHandle();
        WorldServer world = ((CraftWorld) viewer.getWorld()).getHandle();

        EntityPlayer fakeEntity = new EntityPlayer(
                MinecraftServer.getServer(),
                world,
                profile,
                new PlayerInteractManager(world));
        fakeEntity.d(entityId);
        fakeEntity.setLocation(x, y, z, yaw, pitch);

        PacketPlayOutPlayerInfo addPacket = new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, fakeEntity);
        handle.playerConnection.sendPacket(addPacket);

        PacketPlayOutNamedEntitySpawn spawnPacket = new PacketPlayOutNamedEntitySpawn(fakeEntity);
        handle.playerConnection.sendPacket(spawnPacket);

        sendMetadata(viewer);
    }

    public void despawn(Player viewer) {
        if (!viewers.remove(viewer.getUniqueId()))
            return;

        EntityPlayer handle = ((CraftPlayer) viewer).getHandle();

        PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(entityId);
        handle.playerConnection.sendPacket(destroyPacket);

        WorldServer world = ((CraftWorld) viewer.getWorld()).getHandle();
        EntityPlayer fakeEntity = new EntityPlayer(
                MinecraftServer.getServer(),
                world,
                profile,
                new PlayerInteractManager(world));

        PacketPlayOutPlayerInfo removePacket = new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, fakeEntity);
        handle.playerConnection.sendPacket(removePacket);
    }

    public void updatePosition(double newX, double newY, double newZ, float newYaw, float newPitch, boolean onGround) {
        double deltaX = newX - this.x;
        double deltaY = newY - this.y;
        double deltaZ = newZ - this.z;

        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.yaw = newYaw;
        this.pitch = newPitch;
        this.onGround = onGround;

        boolean useTeleport = Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4 || Math.abs(deltaZ) > 4;

        for (UUID viewerUuid : viewers) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer == null || !viewer.isOnline())
                continue;

            EntityPlayer handle = ((CraftPlayer) viewer).getHandle();

            if (useTeleport) {
                PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(
                        entityId,
                        MathHelper.floor(newX * 32.0D),
                        MathHelper.floor(newY * 32.0D),
                        MathHelper.floor(newZ * 32.0D),
                        (byte) ((int) (newYaw * 256.0F / 360.0F)),
                        (byte) ((int) (newPitch * 256.0F / 360.0F)),
                        onGround);
                handle.playerConnection.sendPacket(teleportPacket);
            } else {
                PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook movePacket = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(
                        entityId,
                        (byte) MathHelper.floor(deltaX * 32.0D),
                        (byte) MathHelper.floor(deltaY * 32.0D),
                        (byte) MathHelper.floor(deltaZ * 32.0D),
                        (byte) ((int) (newYaw * 256.0F / 360.0F)),
                        (byte) ((int) (newPitch * 256.0F / 360.0F)),
                        onGround);
                handle.playerConnection.sendPacket(movePacket);
            }

            PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation();
            setField(headPacket, "a", entityId);
            setField(headPacket, "b", (byte) ((int) (newYaw * 256.0F / 360.0F)));
            handle.playerConnection.sendPacket(headPacket);
        }
    }

    public void updateLook(float newYaw, float newPitch) {
        this.yaw = newYaw;
        this.pitch = newPitch;

        for (UUID viewerUuid : viewers) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer == null || !viewer.isOnline())
                continue;

            EntityPlayer handle = ((CraftPlayer) viewer).getHandle();

            PacketPlayOutEntity.PacketPlayOutEntityLook lookPacket = new PacketPlayOutEntity.PacketPlayOutEntityLook(
                    entityId,
                    (byte) ((int) (newYaw * 256.0F / 360.0F)),
                    (byte) ((int) (newPitch * 256.0F / 360.0F)),
                    onGround);
            handle.playerConnection.sendPacket(lookPacket);

            PacketPlayOutEntityHeadRotation headPacket = new PacketPlayOutEntityHeadRotation();
            setField(headPacket, "a", entityId);
            setField(headPacket, "b", (byte) ((int) (newYaw * 256.0F / 360.0F)));
            handle.playerConnection.sendPacket(headPacket);
        }
    }

    public void playAnimation(int animationType) {
        for (UUID viewerUuid : viewers) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer == null || !viewer.isOnline())
                continue;

            EntityPlayer handle = ((CraftPlayer) viewer).getHandle();

            PacketPlayOutAnimation animPacket = new PacketPlayOutAnimation();
            setField(animPacket, "a", entityId);
            setField(animPacket, "b", animationType);
            handle.playerConnection.sendPacket(animPacket);
        }
    }

    public void updateSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        broadcastMetadata();
    }

    public void updateSprinting(boolean sprinting) {
        this.sprinting = sprinting;
        broadcastMetadata();
    }

    public void updateFlying(boolean flying) {
        this.flying = flying;
        broadcastMetadata();
    }

    public void sendEquipment(Player viewer, int slot, net.minecraft.server.v1_8_R3.ItemStack item) {
        EntityPlayer handle = ((CraftPlayer) viewer).getHandle();
        PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(entityId, slot, item);
        handle.playerConnection.sendPacket(packet);
    }

    public void broadcastEquipment(int slot, net.minecraft.server.v1_8_R3.ItemStack item) {
        for (UUID viewerUuid : viewers) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer == null || !viewer.isOnline())
                continue;
            sendEquipment(viewer, slot, item);
        }
    }

    private void sendMetadata(Player viewer) {
        EntityPlayer handle = ((CraftPlayer) viewer).getHandle();

        byte flags = 0;
        if (sneaking)
            flags |= 0x02;
        if (sprinting)
            flags |= 0x08;

        DataWatcher watcher = new DataWatcher(null);
        watcher.a(0, flags);
        watcher.a(10, (byte) 127);

        PacketPlayOutEntityMetadata metaPacket = new PacketPlayOutEntityMetadata(entityId, watcher, true);
        handle.playerConnection.sendPacket(metaPacket);
    }

    private void broadcastMetadata() {
        for (UUID viewerUuid : viewers) {
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer == null || !viewer.isOnline())
                continue;
            sendMetadata(viewer);
        }
    }

    public boolean isViewedBy(UUID viewerUuid) {
        return viewers.contains(viewerUuid);
    }

    public void removeViewer(UUID viewerUuid) {
        viewers.remove(viewerUuid);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

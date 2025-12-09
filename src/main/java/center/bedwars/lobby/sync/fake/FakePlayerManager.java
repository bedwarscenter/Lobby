package center.bedwars.lobby.sync.fake;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.visibility.IPlayerVisibilityService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class FakePlayerManager {

    private static final int BASE_ENTITY_ID = 100000;
    private final AtomicInteger entityIdCounter = new AtomicInteger(BASE_ENTITY_ID);

    @Getter
    private final Map<UUID, FakePlayer> fakePlayers = new ConcurrentHashMap<>();

    private final Lobby plugin;
    private final Provider<IPlayerVisibilityService> visibilityServiceProvider;

    @Inject
    public FakePlayerManager(Lobby plugin, Provider<IPlayerVisibilityService> visibilityServiceProvider) {
        this.plugin = plugin;
        this.visibilityServiceProvider = visibilityServiceProvider;
    }

    public FakePlayer createFakePlayer(UUID uuid, String name, String texture, String signature, byte sourceLobbyId,
            double x, double y, double z, float yaw, float pitch) {
        if (Bukkit.getPlayer(uuid) != null) {
            return null;
        }

        FakePlayer existing = fakePlayers.get(uuid);
        if (existing != null) {
            existing.setSourceLobbyId(sourceLobbyId);
            existing.setLastUpdate(System.currentTimeMillis());
            existing.setX(x);
            existing.setY(y);
            existing.setZ(z);
            existing.setYaw(yaw);
            existing.setPitch(pitch);
            return existing;
        }

        int entityId = entityIdCounter.getAndIncrement();
        FakePlayer fakePlayer = new FakePlayer(uuid, name, texture, signature, entityId, sourceLobbyId);
        fakePlayer.setX(x);
        fakePlayer.setY(y);
        fakePlayer.setZ(z);
        fakePlayer.setYaw(yaw);
        fakePlayer.setPitch(pitch);
        fakePlayers.put(uuid, fakePlayer);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (shouldShowTo(viewer, uuid)) {
                    fakePlayer.spawn(viewer);
                }
            }
        });

        return fakePlayer;
    }

    public void removeFakePlayer(UUID uuid) {
        FakePlayer fakePlayer = fakePlayers.remove(uuid);
        if (fakePlayer == null)
            return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                fakePlayer.despawn(viewer);
            }
        });
    }

    public FakePlayer getFakePlayer(UUID uuid) {
        return fakePlayers.get(uuid);
    }

    public void spawnAllFor(Player viewer) {
        if (viewer == null)
            return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!viewer.isOnline())
                return;

            for (FakePlayer fakePlayer : fakePlayers.values()) {
                if (shouldShowTo(viewer, fakePlayer.getUuid())) {
                    fakePlayer.spawn(viewer);
                }
            }
        }, 20L);
    }

    public void despawnAllFor(Player viewer) {
        if (viewer == null)
            return;

        for (FakePlayer fakePlayer : fakePlayers.values()) {
            fakePlayer.despawn(viewer);
        }
    }

    public void hideAllFakePlayers(Player viewer) {
        if (viewer == null)
            return;

        for (FakePlayer fakePlayer : fakePlayers.values()) {
            fakePlayer.despawn(viewer);
        }
    }

    public void showAllFakePlayers(Player viewer) {
        if (viewer == null)
            return;

        for (FakePlayer fakePlayer : fakePlayers.values()) {
            if (shouldShowTo(viewer, fakePlayer.getUuid())) {
                fakePlayer.spawn(viewer);
            }
        }
    }

    public void updatePosition(UUID uuid, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.updatePosition(x, y, z, yaw, pitch, onGround);
    }

    public void updateLook(UUID uuid, float yaw, float pitch) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.updateLook(yaw, pitch);
    }

    public void playAnimation(UUID uuid, int animationType) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.playAnimation(animationType);
    }

    public void updateSneaking(UUID uuid, boolean sneaking) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.updateSneaking(sneaking);
    }

    public void updateSprinting(UUID uuid, boolean sprinting) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.updateSprinting(sprinting);
    }

    public void updateFlying(UUID uuid, boolean flying) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.updateFlying(flying);
    }

    public void updateHeldItem(UUID uuid, int slot) {
        FakePlayer fakePlayer = fakePlayers.get(uuid);
        if (fakePlayer == null)
            return;

        fakePlayer.setHeldItemSlot(slot);
    }

    public void cleanup() {
        for (UUID uuid : fakePlayers.keySet()) {
            removeFakePlayer(uuid);
        }
        fakePlayers.clear();
    }

    public void cleanupStale(long timeoutMs) {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, FakePlayer> entry : fakePlayers.entrySet()) {
            if (now - entry.getValue().getLastUpdate() > timeoutMs) {
                removeFakePlayer(entry.getKey());
            }
        }
    }

    public Collection<FakePlayer> getAllFakePlayers() {
        return fakePlayers.values();
    }

    public boolean hasFakePlayer(UUID uuid) {
        return fakePlayers.containsKey(uuid);
    }

    private boolean shouldShowTo(Player viewer, UUID fakePlayerUuid) {
        if (viewer.getUniqueId().equals(fakePlayerUuid)) {
            return false;
        }

        IPlayerVisibilityService visibilityService = visibilityServiceProvider.get();
        if (visibilityService != null && visibilityService.isHidden(viewer)) {
            return false;
        }

        return true;
    }
}

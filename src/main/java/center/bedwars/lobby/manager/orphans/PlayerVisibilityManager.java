package center.bedwars.lobby.manager.orphans;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerVisibilityManager extends Manager {

    private static final String ENTITY_HANDLER_NAME = "bwl_visibility";
    private static final String SOUND_HANDLER_NAME = "bwl_visibility_sound";
    private static final Field SOUND_NAME_FIELD = resolveSoundField("a");
    private static final Field SOUND_X_FIELD = resolveSoundField("b");
    private static final Field SOUND_Y_FIELD = resolveSoundField("c");
    private static final Field SOUND_Z_FIELD = resolveSoundField("d");

    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private final Map<UUID, Long> toggleCooldowns = new HashMap<>();

    @Override
    protected void onLoad() {
    }

    @Override
    protected void onUnload() {
        hiddenPlayers.clear();
        toggleCooldowns.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            removeInterceptors(player);
        }
    }

    public boolean toggleVisibilityWithCooldown(Player player) {
        if (!isCooldownReady(player)) {
            return false;
        }
        toggleVisibility(player);
        registerToggle(player);
        return true;
    }

    public void toggleVisibility(Player player) {
        if (hiddenPlayers.contains(player.getUniqueId())) {
            showPlayers(player);
        } else {
            hidePlayers(player);
        }
    }

    public long getRemainingCooldown(Player player) {
        if (SettingsConfiguration.VISIBILITY.TOGGLE_COOLDOWN_MILLIS <= 0) {
            return 0L;
        }
        Long lastToggle = toggleCooldowns.get(player.getUniqueId());
        if (lastToggle == null) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - lastToggle;
        long remaining = SettingsConfiguration.VISIBILITY.TOGGLE_COOLDOWN_MILLIS - elapsed;
        return Math.max(0L, remaining);
    }

    public void hidePlayers(Player player) {
        if (hiddenPlayers.contains(player.getUniqueId())) {
            return;
        }

        hiddenPlayers.add(player.getUniqueId());

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;

            if (target.hasPermission("bedwarslobby.staff")) {
                continue;
            }

            player.hidePlayer(target);
        }

        setupEntityInterceptor(player);
        setupSoundInterceptor(player);
    }

    public void showPlayers(Player player) {
        if (!hiddenPlayers.contains(player.getUniqueId())) {
            return;
        }

        hiddenPlayers.remove(player.getUniqueId());
        removeInterceptors(player);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            player.showPlayer(target);
        }
    }

    private void setupEntityInterceptor(Player player) {
        NMSHelper.cancelPacketIf(
                player,
                ENTITY_HANDLER_NAME,
                PacketPlayOutNamedEntitySpawn.class,
                (p, packet) -> {
                    if (p == null) {
                        return false;
                    }
                    try {
                        int entityId = (int) packet.getClass().getField("a").get(packet);

                        for (Player target : Bukkit.getOnlinePlayers()) {
                            if (target.getEntityId() == entityId) {
                                if (target.hasPermission("bedwarslobby.staff")) {
                                    return false;
                                }
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                },
                false,
                true
        );
    }

    private void setupSoundInterceptor(Player player) {
        if (!SettingsConfiguration.VISIBILITY.MUTE_SOUNDS_WHEN_HIDDEN) {
            return;
        }

        NMSHelper.cancelPacketIf(
                player,
                SOUND_HANDLER_NAME,
                PacketPlayOutNamedSoundEffect.class,
                this::shouldMuteSound,
                false,
                true
        );
    }

    private boolean shouldMuteSound(Player viewer, PacketPlayOutNamedSoundEffect packet) {
        if (!hiddenPlayers.contains(viewer.getUniqueId())) {
            return false;
        }
        if (!SettingsConfiguration.VISIBILITY.MUTE_SOUNDS_WHEN_HIDDEN) {
            return false;
        }

        String soundName = getSoundName(packet);
        if (soundName == null || !isMutedSound(soundName)) {
            return false;
        }

        Location soundLocation = getSoundLocation(packet, viewer.getWorld());
        if (soundLocation == null) {
            return false;
        }

        double radius = SettingsConfiguration.VISIBILITY.SOUND_RADIUS;
        double radiusSquared = radius * radius;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) {
                continue;
            }
            if (!shouldHideTarget(target)) {
                continue;
            }
            if (!target.getWorld().equals(soundLocation.getWorld())) {
                continue;
            }
            if (target.getLocation().distanceSquared(soundLocation) <= radiusSquared) {
                return true;
            }
        }

        return false;
    }

    private String getSoundName(PacketPlayOutNamedSoundEffect packet) {
        if (SOUND_NAME_FIELD == null) {
            return null;
        }
        try {
            Object value = SOUND_NAME_FIELD.get(packet);
            return value != null ? value.toString() : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private Location getSoundLocation(PacketPlayOutNamedSoundEffect packet, World world) {
        if (SOUND_X_FIELD == null || SOUND_Y_FIELD == null || SOUND_Z_FIELD == null) {
            return null;
        }
        try {
            double x = ((int) SOUND_X_FIELD.get(packet)) / 8.0d;
            double y = ((int) SOUND_Y_FIELD.get(packet)) / 8.0d;
            double z = ((int) SOUND_Z_FIELD.get(packet)) / 8.0d;
            return new Location(world, x, y, z);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private boolean isMutedSound(String soundName) {
        if (soundName == null) {
            return false;
        }
        if (SettingsConfiguration.VISIBILITY.MUTED_SOUNDS == null) {
            return false;
        }
        for (String muted : SettingsConfiguration.VISIBILITY.MUTED_SOUNDS) {
            if (muted != null && muted.equalsIgnoreCase(soundName)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldHideTarget(Player target) {
        return !target.hasPermission("bedwarslobby.staff");
    }

    private void removeInterceptors(Player player) {
        NMSHelper.removePacketListener(player, ENTITY_HANDLER_NAME);
        NMSHelper.removePacketListener(player, SOUND_HANDLER_NAME);
    }

    private static Field resolveSoundField(String name) {
        try {
            Field field = PacketPlayOutNamedSoundEffect.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    public boolean isHidden(Player player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    public void handlePlayerJoin(Player joiningPlayer) {
        Bukkit.getScheduler().runTaskLater(Lobby.getINSTANCE(), () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(joiningPlayer)) continue;

                if (hiddenPlayers.contains(viewer.getUniqueId())) {
                    if (!joiningPlayer.hasPermission("bedwarslobby.staff")) {
                        viewer.hidePlayer(joiningPlayer);
                    }
                }
            }

            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target.equals(joiningPlayer)) continue;

                if (hiddenPlayers.contains(joiningPlayer.getUniqueId())) {
                    if (!target.hasPermission("bedwarslobby.staff")) {
                        joiningPlayer.hidePlayer(target);
                    }
                }
            }
        }, 1L);
    }

    public void handlePlayerQuit(Player player) {
        hiddenPlayers.remove(player.getUniqueId());
        removeInterceptors(player);
        toggleCooldowns.remove(player.getUniqueId());
    }

    private boolean isCooldownReady(Player player) {
        if (SettingsConfiguration.VISIBILITY.TOGGLE_COOLDOWN_MILLIS <= 0) {
            return true;
        }
        Long lastToggle = toggleCooldowns.get(player.getUniqueId());
        if (lastToggle == null) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - lastToggle;
        return elapsed >= SettingsConfiguration.VISIBILITY.TOGGLE_COOLDOWN_MILLIS;
    }

    private void registerToggle(Player player) {
        if (SettingsConfiguration.VISIBILITY.TOGGLE_COOLDOWN_MILLIS <= 0) {
            return;
        }
        toggleCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
package center.bedwars.lobby.util;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@UtilityClass
public class SpawnUtil {

    public Location getConfiguredSpawn() {
        String locationString = SettingsConfiguration.SPAWN_LOCATION;
        if (locationString == null || locationString.trim().isEmpty()) {
            return fallbackWorldSpawn();
        }

        String[] parts = locationString.split(";");
        if (parts.length != 6) {
            Lobby.getInstance().getLogger()
                    .warning("[SpawnUtil] Invalid spawn format. Falling back to default world spawn.");
            return fallbackWorldSpawn();
        }

        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            String worldName = parts[3];
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Lobby.getInstance().getLogger().warning(
                        "[SpawnUtil] World '" + worldName + "' not found. Falling back to default world spawn.");
                return fallbackWorldSpawn();
            }

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException exception) {
            Lobby.getInstance().getLogger()
                    .warning("[SpawnUtil] Failed to parse spawn location: " + exception.getMessage());
            return fallbackWorldSpawn();
        }
    }

    public boolean teleportToSpawn(Player player) {
        return teleportToSpawn(player, false);
    }

    public boolean teleportToSpawn(Player player, boolean keepOrientation) {
        Location spawn = getConfiguredSpawn();
        if (spawn == null) {
            return false;
        }

        Location target = spawn.clone();
        if (keepOrientation) {
            target.setYaw(player.getLocation().getYaw());
            target.setPitch(player.getLocation().getPitch());
        }

        player.setFallDistance(0F);
        player.setVelocity(player.getVelocity().multiply(0));
        player.teleport(target);

        Bukkit.getScheduler().runTaskLater(Lobby.getInstance(), () -> {
            player.setFallDistance(0F);
            player.setVelocity(player.getVelocity().multiply(0));
        }, 1L);

        return true;
    }

    private Location fallbackWorldSpawn() {
        World primaryWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        if (primaryWorld == null) {
            return null;
        }
        Location spawn = primaryWorld.getSpawnLocation();
        return new Location(primaryWorld, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
    }
}
package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
@SuppressWarnings({"unused"})
public final class PlaceholderAPIDependency {

    private static final String DEPENDENCY_NAME = "PlaceholderAPI";

    private final boolean present;

    public PlaceholderAPIDependency() {
        boolean isPresent = false;

        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            isPresent = Bukkit.getPluginManager().getPlugin(DEPENDENCY_NAME) != null;
        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
        }

        this.present = isPresent;
    }

    public String getDependencyName() {
        return DEPENDENCY_NAME;
    }

    public boolean isApiAvailable() {
        return present;
    }
}
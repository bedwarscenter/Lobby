package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
@SuppressWarnings({"unused"})
public final class DecentHologramsDependency {

    private static final String DEPENDENCY_NAME = "DecentHolograms";

    private final boolean present;

    public DecentHologramsDependency() {
        boolean isPresent = false;

        try {
            Class.forName("eu.decentsoftware.holograms.api.DHAPI");
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
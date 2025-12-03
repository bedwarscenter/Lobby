package center.bedwars.lobby.dependency.dependencies;

import org.bukkit.Bukkit;

public class AlonsoLevelsDependency {

    private static final String DEPENDENCY_NAME = "AlonsoLevels";

    private final boolean present;

    public AlonsoLevelsDependency() {
        boolean isPresent = false;

        try {
            Class.forName("com.alonsoaliaga.alonsolevels.api.AlonsoLevelsAPI");
            if (Bukkit.getPluginManager().getPlugin(DEPENDENCY_NAME) != null) {
                isPresent = true;
            }
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
package center.bedwars.lobby.dependency;

import org.bukkit.Bukkit;

public abstract class AbstractDependency implements IDependency {

    private final String dependencyName;
    private final boolean present;

    protected AbstractDependency(String dependencyName, String classToCheck) {
        this.dependencyName = dependencyName;
        this.present = checkPresence(classToCheck);
    }

    private boolean checkPresence(String classToCheck) {
        try {
            Class.forName(classToCheck);
            return Bukkit.getPluginManager().getPlugin(dependencyName) != null;
        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public String getDependencyName() {
        return dependencyName;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean isApiAvailable() {
        return present;
    }
}

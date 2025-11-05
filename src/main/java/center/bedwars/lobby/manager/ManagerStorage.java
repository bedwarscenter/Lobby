package center.bedwars.lobby.manager;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"unused"})
public class ManagerStorage {
    private final Map<Class<? extends Manager>, Manager> managers = new LinkedHashMap<>();

    public void register(Manager manager) {
        managers.put(manager.getClass(), manager);
    }

    public void loadManager(Manager manager) {
        try {
            manager.load();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load manager: " + manager.getClass().getSimpleName(), e);
        }
    }

    public void registerAndLoad(Manager manager) {
        register(manager);
        loadManager(manager);
    }

    public void setAllWaiting() {
        managers.values().forEach(Manager::setWaiting);
    }

    public void finishAll() {
        managers.values().forEach(manager -> {
            try {
                manager.finishLoading();
            } catch (Exception e) {
                throw new RuntimeException("Failed to finish manager: " + manager.getClass().getSimpleName(), e);
            }
        });
    }

    public void unloadAll() {
        managers.values().forEach(manager -> {
            try {
                manager.unload();
            } catch (Exception e) {
                throw new RuntimeException("Failed to unload manager: " + manager.getClass().getSimpleName(), e);
            }
        });
        managers.clear();
    }

    public <T extends Manager> T getManager(Class<T> managerClass) {
        return (T) managers.get(managerClass);
    }
}
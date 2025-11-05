package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
@SuppressWarnings({"unused"})
public final class MBedwarsDependency {

    private static final int SUPPORTED_API_VERSION = 205;
    private static final String SUPPORTED_VERSION_NAME = "5.5.5";
    private static final String DEPENDENCY_NAME = "MBedwars";

    private final boolean present;
    private final Object api;

    public MBedwarsDependency() throws Exception {
        Object tempApi;
        boolean isPresent;

        try {
            Class<?> apiClass = Class.forName("de.marcely.bedwars.api.BedwarsAPI");
            int apiVersion = (int) apiClass.getMethod("getAPIVersion").invoke(null);

            if (apiVersion < SUPPORTED_API_VERSION) {
                throw new IllegalStateException("MBedwars API version too old");
            }

            tempApi = apiClass;
            isPresent = true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Sorry, your installed version of MBedwars is not supported. Please install at least v" + SUPPORTED_VERSION_NAME);
            throw e;
        }

        this.api = tempApi;
        this.present = isPresent;
    }

    public String getDependencyName() {
        return DEPENDENCY_NAME;
    }

    public boolean isApiAvailable() {
        return present && api != null;
    }
}
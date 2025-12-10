package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.IDependency;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public final class MBedwarsDependency implements IDependency {

    private static final int SUPPORTED_API_VERSION = 205;
    private static final String SUPPORTED_VERSION_NAME = "5.5.5";
    private static final String API_CLASS = "de.marcely.bedwars.api.BedwarsAPI";

    private final boolean present;
    private final Object api;

    public MBedwarsDependency() throws Exception {
        Object tempApi;
        boolean isPresent;

        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            int apiVersion = (int) apiClass.getMethod("getAPIVersion").invoke(null);

            if (apiVersion < SUPPORTED_API_VERSION) {
                throw new IllegalStateException("MBedwars API version too old");
            }

            tempApi = apiClass;
            isPresent = true;
        } catch (Exception e) {
            Bukkit.getLogger()
                    .warning("Sorry, your installed version of MBedwars is not supported. Please install at least v"
                            + SUPPORTED_VERSION_NAME);
            throw e;
        }

        this.api = tempApi;
        this.present = isPresent;
    }

    @Override
    public String getDependencyName() {
        return DependencyConstants.MBEDWARS;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean isApiAvailable() {
        return present && api != null;
    }
}

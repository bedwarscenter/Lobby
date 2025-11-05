package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import xyz.refinedev.phoenix.CommonPlatform;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.PlatformGetter;

@Getter
@SuppressWarnings({"unused"})
public final class PhoenixDependency {

    private static final String DEPENDENCY_NAME = "Phoenix";

    private final boolean present;
    private final Phoenix api;
    private final CommonPlatform commonPlatform;

    public PhoenixDependency() {
        Phoenix tempApi;
        CommonPlatform tempPlatform;
        boolean isPresent = false;

        try {
            tempPlatform = PlatformGetter.getInstance();
        } catch (NoClassDefFoundError | Exception ignored) {
            tempPlatform = null;
        }

        try {
            tempApi = Phoenix.INSTANCE;
            isPresent = (tempApi != null);
        } catch (NoClassDefFoundError | Exception ignored) {
            tempApi = null;
        }

        this.commonPlatform = tempPlatform;
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
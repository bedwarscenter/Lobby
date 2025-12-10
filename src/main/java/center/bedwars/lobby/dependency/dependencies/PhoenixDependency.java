package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.IDependency;
import lombok.Getter;
import xyz.refinedev.phoenix.CommonPlatform;
import xyz.refinedev.phoenix.Phoenix;
import xyz.refinedev.phoenix.PlatformGetter;

@Getter
public final class PhoenixDependency implements IDependency {

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

    @Override
    public String getDependencyName() {
        return DependencyConstants.PHOENIX;
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

package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.AbstractDependency;

public final class AlonsoLevelsDependency extends AbstractDependency {

    private static final String API_CLASS = "com.alonsoaliaga.alonsolevels.api.AlonsoLevelsAPI";

    public AlonsoLevelsDependency() {
        super(DependencyConstants.ALONSO_LEVELS, API_CLASS);
    }
}

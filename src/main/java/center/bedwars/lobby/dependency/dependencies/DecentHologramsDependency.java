package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.AbstractDependency;
import lombok.Getter;

@Getter
public final class DecentHologramsDependency extends AbstractDependency {

    private static final String API_CLASS = "eu.decentsoftware.holograms.api.DHAPI";

    public DecentHologramsDependency() {
        super(DependencyConstants.DECENT_HOLOGRAMS, API_CLASS);
    }
}

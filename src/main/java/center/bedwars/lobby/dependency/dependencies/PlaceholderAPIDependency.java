package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.AbstractDependency;
import lombok.Getter;

@Getter
public final class PlaceholderAPIDependency extends AbstractDependency {

    private static final String API_CLASS = "me.clip.placeholderapi.PlaceholderAPI";

    public PlaceholderAPIDependency() {
        super(DependencyConstants.PLACEHOLDER_API, API_CLASS);
    }
}

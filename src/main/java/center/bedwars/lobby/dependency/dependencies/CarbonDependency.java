package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.IDependency;
import lombok.Getter;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;

import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class CarbonDependency implements IDependency {

    private static final String API_CLASS = "xyz.refinedev.spigot.features.chunk.IChunkAPI";
    private static final Logger LOGGER = Logger.getLogger(CarbonDependency.class.getName());

    private final boolean present;
    private final IChunkAPI chunkRegistry;

    public CarbonDependency() {
        IChunkAPI tempChunkRegistry = null;
        boolean isPresent = false;

        try {
            Class.forName(API_CLASS);
            tempChunkRegistry = IChunkAPI.instance();
            if (tempChunkRegistry != null) {
                isPresent = true;
            } else {
                LOGGER.warning("[" + DependencyConstants.CARBON + "] IChunkAPI instance is null!");
            }
        } catch (NoClassDefFoundError e) {
            LOGGER.log(Level.WARNING, "[" + DependencyConstants.CARBON + "] NoClassDefFoundError: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "[" + DependencyConstants.CARBON + "] ClassNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[" + DependencyConstants.CARBON + "] Unexpected error while loading: ", e);
        }

        this.present = isPresent;
        this.chunkRegistry = tempChunkRegistry;
    }

    @Override
    public String getDependencyName() {
        return DependencyConstants.CARBON;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean isApiAvailable() {
        return present && chunkRegistry != null;
    }
}

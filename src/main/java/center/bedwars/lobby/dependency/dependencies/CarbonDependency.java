package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;

import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
@SuppressWarnings("unused")
public class CarbonDependency {

    private static final String DEPENDENCY_NAME = "Carbon";
    private static final Logger LOGGER = Logger.getLogger(CarbonDependency.class.getName());

    private final boolean present;
    private final IChunkAPI chunkRegistry;

    public CarbonDependency() {
        IChunkAPI tempChunkRegistry = null;
        boolean isPresent = false;

        try {
            LOGGER.info("[" + DEPENDENCY_NAME + "] Attempting to load dependency...");

            Class.forName("xyz.refinedev.spigot.features.chunk.IChunkAPI");
            LOGGER.info("[" + DEPENDENCY_NAME + "] IChunkAPI class found!");

            tempChunkRegistry = IChunkAPI.instance();
            if (tempChunkRegistry != null) {
                LOGGER.info("[" + DEPENDENCY_NAME + "] IChunkAPI instance loaded successfully!");
                isPresent = true;
            } else {
                LOGGER.warning("[" + DEPENDENCY_NAME + "] IChunkAPI instance is null!");
            }

        } catch (NoClassDefFoundError e) {
            LOGGER.log(Level.WARNING, "[" + DEPENDENCY_NAME + "] NoClassDefFoundError: " + e.getMessage());
            LOGGER.warning("[" + DEPENDENCY_NAME + "] Make sure Carbon plugin is installed!");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "[" + DEPENDENCY_NAME + "] ClassNotFoundException: " + e.getMessage());
            LOGGER.warning("[" + DEPENDENCY_NAME + "] Carbon plugin is not loaded!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[" + DEPENDENCY_NAME + "] Unexpected error while loading: ", e);
        }

        this.present = isPresent;
        this.chunkRegistry = tempChunkRegistry;
    }

    public String getDependencyName() {
        return DEPENDENCY_NAME;
    }

    public boolean isApiAvailable() {
        return present && chunkRegistry != null;
    }
}
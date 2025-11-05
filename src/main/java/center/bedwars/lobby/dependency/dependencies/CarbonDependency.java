package center.bedwars.lobby.dependency.dependencies;


import lombok.Getter;
import xyz.refinedev.spigot.features.chunk.IChunkAPI;

@Getter
@SuppressWarnings("unused")
public class CarbonDependency {

    private static final String DEPENDENCY_NAME = "Carbon";

    private final boolean present;
    private final IChunkAPI chunkRegistry;

    public CarbonDependency() {
        IChunkAPI tempRegistry = null;
        boolean isPresent = false;

        try {
            Class.forName("xyz.refinedev.spigot.features.chunk.IChunkAPI");
            tempRegistry = IChunkAPI.instance();
            isPresent = (tempRegistry != null);
        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {

        }

        this.present = isPresent;
        this.chunkRegistry = tempRegistry;
    }

    public String getDependencyName() {
        return DEPENDENCY_NAME;
    }

    public boolean isApiAvailable() {
        return present && chunkRegistry != null;
    }

}

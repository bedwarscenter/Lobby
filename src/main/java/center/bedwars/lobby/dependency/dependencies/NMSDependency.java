package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import org.bukkit.Bukkit;

@Getter
@SuppressWarnings({"unused"})
public final class NMSDependency {

    private static final String DEPENDENCY_NAME = "NMS";
    private static final String NMS_VERSION = "v1_8_R3";

    private final boolean present;
    private final String version;
    private final MinecraftServer minecraftServer;

    public NMSDependency() {
        boolean isPresent = false;
        MinecraftServer server = null;

        try {
            server = MinecraftServer.getServer();
            isPresent = (server != null);
        } catch (Exception | NoClassDefFoundError e) {
            Bukkit.getLogger().warning("Failed to initialize NMS: " + e.getMessage());
        }

        this.version = NMS_VERSION;
        this.minecraftServer = server;
        this.present = isPresent;
    }

    public String getDependencyName() {
        return DEPENDENCY_NAME;
    }

    public boolean isApiAvailable() {
        return present && minecraftServer != null;
    }

    public String getSupportedVersion() {
        return NMS_VERSION;
    }
}
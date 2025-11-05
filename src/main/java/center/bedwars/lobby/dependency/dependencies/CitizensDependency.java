package center.bedwars.lobby.dependency.dependencies;

import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;

@Getter
@SuppressWarnings({"unused"})
public final class CitizensDependency {

    private static final String DEPENDENCY_NAME = "Citizens";

    private final boolean present;
    private final NPCRegistry npcRegistry;

    public CitizensDependency() {
        NPCRegistry tempRegistry = null;
        boolean isPresent = false;

        try {
            Class.forName("net.citizensnpcs.api.CitizensAPI");
            if (Bukkit.getPluginManager().getPlugin(DEPENDENCY_NAME) != null) {
                tempRegistry = CitizensAPI.getNPCRegistry();
                isPresent = (tempRegistry != null);
            }
        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
        }

        this.npcRegistry = tempRegistry;
        this.present = isPresent;
    }

    public String getDependencyName() {
        return DEPENDENCY_NAME;
    }

    public boolean isApiAvailable() {
        return present && npcRegistry != null;
    }
}
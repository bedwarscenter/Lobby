package center.bedwars.lobby.dependency.dependencies;

import center.bedwars.lobby.constant.DependencyConstants;
import center.bedwars.lobby.dependency.IDependency;
import lol.pyr.znpcsplus.api.NpcApi;
import lol.pyr.znpcsplus.api.NpcApiProvider;
import lol.pyr.znpcsplus.api.entity.EntityPropertyRegistry;
import lol.pyr.znpcsplus.api.npc.NpcRegistry;
import lol.pyr.znpcsplus.api.npc.NpcTypeRegistry;
import lol.pyr.znpcsplus.api.skin.SkinDescriptorFactory;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public final class ZNPCsPlusDependency implements IDependency {

    private static final String API_CLASS = "lol.pyr.znpcsplus.api.NpcApiProvider";

    private final boolean present;
    private final NpcApi npcApi;

    public ZNPCsPlusDependency() {
        NpcApi tempApi = null;
        boolean isPresent = false;

        try {
            Class.forName(API_CLASS);
            if (Bukkit.getPluginManager().getPlugin(DependencyConstants.ZNPCS_PLUS) != null) {
                tempApi = NpcApiProvider.get();
                isPresent = (tempApi != null);
            }
        } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
        }

        this.npcApi = tempApi;
        this.present = isPresent;
    }

    @Override
    public String getDependencyName() {
        return DependencyConstants.ZNPCS_PLUS;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean isApiAvailable() {
        return present && npcApi != null;
    }

    public NpcRegistry getNpcRegistry() {
        return npcApi != null ? npcApi.getNpcRegistry() : null;
    }

    public NpcTypeRegistry getNpcTypeRegistry() {
        return npcApi != null ? npcApi.getNpcTypeRegistry() : null;
    }

    public EntityPropertyRegistry getPropertyRegistry() {
        return npcApi != null ? npcApi.getPropertyRegistry() : null;
    }

    public SkinDescriptorFactory getSkinDescriptorFactory() {
        return npcApi != null ? npcApi.getSkinDescriptorFactory() : null;
    }
}

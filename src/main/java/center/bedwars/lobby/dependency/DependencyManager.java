package center.bedwars.lobby.dependency;

import center.bedwars.lobby.dependency.dependencies.*;
import center.bedwars.lobby.expansion.PlayerExpansion;
import center.bedwars.lobby.manager.Manager;
import lombok.Getter;

@Getter
public class DependencyManager extends Manager {

    private CarbonDependency carbon;
    private MBedwarsDependency mbedwars;
    private PhoenixDependency phoenix;
    private PlaceholderAPIDependency placeholderAPI;
    private DecentHologramsDependency decentHolograms;
    private CitizensDependency citizens;
    private NMSDependency nms;

    private PlayerExpansion playerExpansion;

    @Override
    protected void onLoad() throws Exception {
        this.carbon = new CarbonDependency();
        this.mbedwars = new MBedwarsDependency();
        this.phoenix = new PhoenixDependency();
        this.nms = new NMSDependency();
        this.decentHolograms = new DecentHologramsDependency();
        this.citizens = new CitizensDependency();
    }

    @Override
    protected void onFinish() {
        this.placeholderAPI = new PlaceholderAPIDependency();

        if (placeholderAPI.isPresent()) {
            this.playerExpansion = new PlayerExpansion();
            this.playerExpansion.register();
        }
    }

    @Override
    protected void onUnload() {
        if (placeholderAPI != null && placeholderAPI.isPresent()) {
            if (playerExpansion != null) {
                playerExpansion.unregister();
            }
        }
    }
}
package center.bedwars.lobby.dependency;

import center.bedwars.lobby.dependency.dependencies.*;
import center.bedwars.lobby.service.IService;

public interface IDependencyService extends IService {
    CarbonDependency getCarbon();

    MBedwarsDependency getMBedwars();

    PhoenixDependency getPhoenix();

    PlaceholderAPIDependency getPlaceholderAPI();

    DecentHologramsDependency getDecentHolograms();

    ZNPCsPlusDependency getZNPCsPlus();

    NMSDependency getNMS();

    AlonsoLevelsDependency getAlonsoLevels();
}

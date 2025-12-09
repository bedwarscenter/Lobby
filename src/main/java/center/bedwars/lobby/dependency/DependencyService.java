package center.bedwars.lobby.dependency;

import center.bedwars.lobby.dependency.dependencies.*;
import center.bedwars.lobby.service.AbstractService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DependencyService extends AbstractService implements IDependencyService {

    private CarbonDependency carbon;
    private MBedwarsDependency mbedwars;
    private PhoenixDependency phoenix;
    private PlaceholderAPIDependency placeholderAPI;
    private DecentHologramsDependency decentHolograms;
    private CitizensDependency citizens;
    private NMSDependency nms;
    private AlonsoLevelsDependency alonsoLevels;

    @Inject
    public DependencyService() {
    }

    @Override
    protected void onEnable() {
        this.carbon = new CarbonDependency();
        try {
            this.mbedwars = new MBedwarsDependency();
        } catch (Exception ignored) {
        }
        this.phoenix = new PhoenixDependency();
        this.nms = new NMSDependency();
        this.decentHolograms = new DecentHologramsDependency();
        this.citizens = new CitizensDependency();
        this.placeholderAPI = new PlaceholderAPIDependency();
        this.alonsoLevels = new AlonsoLevelsDependency();
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public CarbonDependency getCarbon() {
        return carbon;
    }

    @Override
    public MBedwarsDependency getMBedwars() {
        return mbedwars;
    }

    @Override
    public PhoenixDependency getPhoenix() {
        return phoenix;
    }

    @Override
    public PlaceholderAPIDependency getPlaceholderAPI() {
        return placeholderAPI;
    }

    @Override
    public DecentHologramsDependency getDecentHolograms() {
        return decentHolograms;
    }

    @Override
    public CitizensDependency getCitizens() {
        return citizens;
    }

    @Override
    public NMSDependency getNMS() {
        return nms;
    }

    @Override
    public AlonsoLevelsDependency getAlonsoLevels() {
        return alonsoLevels;
    }
}

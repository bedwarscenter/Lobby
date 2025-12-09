package center.bedwars.lobby.nms;

import center.bedwars.lobby.service.AbstractService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;

@Singleton
public class NMSService extends AbstractService implements INMSService {

    private static final String EXPECTED_VERSION = "v1_8_R3";
    private boolean available = false;
    private String version = "";

    @Inject
    public NMSService() {
    }

    @Override
    protected void onEnable() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        version = packageName.substring(packageName.lastIndexOf('.') + 1);
        available = EXPECTED_VERSION.equals(version);
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getVersion() {
        return version;
    }
}

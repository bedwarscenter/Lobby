package center.bedwars.lobby.nms;

import center.bedwars.lobby.dependency.dependencies.NMSDependency;
import center.bedwars.lobby.manager.Manager;
import lombok.Getter;

@Getter
@SuppressWarnings({"unused"})
public class NMSManager extends Manager {

    private NMSDependency nmsDependency;
    private boolean nmsAvailable;

    @Override
    protected void onLoad() {
        this.nmsDependency = new NMSDependency();
        this.nmsAvailable = nmsDependency.isApiAvailable();
    }

    @Override
    protected void onUnload() {
        this.nmsAvailable = false;
    }

    public boolean isNMSAvailable() {
        return nmsAvailable && nmsDependency != null && nmsDependency.isApiAvailable();
    }

    public NMSHelper getHelper() {
        if (!isNMSAvailable()) {
            throw new IllegalStateException("NMS is not available!");
        }
        return null;
    }
}
package center.bedwars.lobby.nms;

import center.bedwars.lobby.dependency.dependencies.NMSDependency;
import center.bedwars.lobby.manager.Manager;
import lombok.Getter;

@Getter
public final class NMSManager extends Manager {

    private NMSDependency nmsDependency;
    private volatile boolean nmsAvailable;

    @Override
    protected void onLoad() {
        this.nmsDependency = new NMSDependency();
        this.nmsAvailable = nmsDependency.isApiAvailable();
    }

    @Override
    protected void onUnload() {
        NMSHelper.cleanupAll();
        this.nmsAvailable = false;
    }

    public boolean isNMSAvailable() {
        return nmsAvailable && nmsDependency != null && nmsDependency.isApiAvailable();
    }
}
package center.bedwars.lobby.service;

public abstract class AbstractService implements IService {

    private boolean enabled = false;

    @Override
    public final void enable() {
        if (enabled)
            return;
        onEnable();
        enabled = true;
    }

    @Override
    public final void disable() {
        if (!enabled)
            return;
        onDisable();
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    protected abstract void onEnable();

    protected abstract void onDisable();
}

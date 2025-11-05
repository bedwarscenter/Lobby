package center.bedwars.lobby.manager;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

@Getter
@SuppressWarnings({"unused"})
public abstract class Manager {

    public enum ManagerState {
        NOT_INITIALIZED,
        LOADING,
        WAITING,
        LOADED
    }

    private ManagerState state = ManagerState.NOT_INITIALIZED;

    public void load() throws Exception {
        long startTime = System.currentTimeMillis();
        if (state != ManagerState.NOT_INITIALIZED) {
            sendConsoleMessage("&e" + getClass().getSimpleName() + " &balready loaded, skipping.");
            return;
        }

        try {
            state = ManagerState.LOADING;
            onLoad();
            state = ManagerState.LOADED;
            sendSuccessMessage(startTime, "loaded");
        } catch (Exception e) {
            sendFailureMessage(startTime, "load");
            throw e;
        }
    }

    public void setWaiting() {
        if (state == ManagerState.LOADING) {
            state = ManagerState.WAITING;
        }
    }

    public void finishLoading() {
        if (state == ManagerState.WAITING) {
            long startTime = System.currentTimeMillis();
            try {
                onFinish();
                state = ManagerState.LOADED;
                sendSuccessMessage(startTime, "finished");
            } catch (Exception e) {
                sendFailureMessage(startTime, "finish");
                throw e;
            }
        }
    }

    public void unload() throws Exception {
        long startTime = System.currentTimeMillis();
        if (state != ManagerState.LOADED) {
            sendConsoleMessage("&e" + getClass().getSimpleName() + " &balready unloaded, skipping.");
            return;
        }

        try {
            onUnload();
            state = ManagerState.NOT_INITIALIZED;
            sendSuccessMessage(startTime, "unloaded");
        } catch (Exception e) {
            sendFailureMessage(startTime, "unload");
            throw e;
        }
    }

    private void sendConsoleMessage(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize(message));
    }

    private void sendSuccessMessage(long startTime, String operation) {
        long duration = System.currentTimeMillis() - startTime;
        sendConsoleMessage(
                "&e" + getClass().getSimpleName() + " &asuccessfully " + operation + ". &8(&d" + duration + "ms&8)"
        );
    }

    private void sendFailureMessage(long startTime, String operation) {
        long duration = System.currentTimeMillis() - startTime;
        sendConsoleMessage(
                "&cFailed to " + operation + " &e" + getClass().getSimpleName() + ". &8(&d" + duration + "ms&8)"
        );
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    protected abstract void onLoad() throws Exception;

    protected void onFinish() {
    }

    protected abstract void onUnload() throws Exception;
}
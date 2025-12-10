package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.ILobbySyncService;
import com.google.inject.Inject;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.Serializer;
import center.bedwars.lobby.sync.serialization.Serializer.HologramData;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.event.DecentHologramsReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HologramCreationListener implements Listener {

    private final Lobby plugin;
    private final ILobbySyncService syncService;
    private final Set<String> syncedHolograms = ConcurrentHashMap.newKeySet();

    @Inject
    public HologramCreationListener(Lobby plugin, ILobbySyncService syncService) {
        this.plugin = plugin;
        this.syncService = syncService;
        startHologramMonitoring();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDecentHologramsReload(DecentHologramsReloadEvent event) {
        syncedHolograms.clear();
        Bukkit.getScheduler().runTaskLater(plugin, this::syncAllHolograms, 5L);
    }

    private void startHologramMonitoring() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                for (String hologramName : Hologram.getCachedHologramNames()) {
                    if (!syncedHolograms.contains(hologramName)) {
                        Hologram hologram = DHAPI.getHologram(hologramName);
                        if (hologram != null && hologram.isEnabled()) {
                            syncedHolograms.add(hologramName);
                            syncHologram(hologram, SyncEventType.HOLOGRAM_CREATE);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Hologram monitoring error: " + e.getMessage());
            }
        }, 20L, 100L);
    }

    private void syncAllHolograms() {
        for (String hologramName : Hologram.getCachedHologramNames()) {
            Hologram hologram = DHAPI.getHologram(hologramName);
            if (hologram != null && hologram.isEnabled()) {
                syncHologram(hologram, SyncEventType.HOLOGRAM_UPDATE);
            }
        }
    }

    private void syncHologram(Hologram hologram, SyncEventType eventType) {
        try {
            if (hologram.getLocation() == null)
                return;

            String[] lines = hologram.getPage(0).getLines().stream()
                    .map(line -> line.getContent())
                    .toArray(String[]::new);

            HologramData hologramData = new HologramData(
                    hologram.getName(),
                    hologram.getLocation(),
                    lines);

            byte[] serialized = Serializer.serialize(hologramData);
            syncService.broadcastEvent(eventType, serialized);
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Failed to sync hologram " + hologram.getName() + ": " + e.getMessage());
        }
    }
}

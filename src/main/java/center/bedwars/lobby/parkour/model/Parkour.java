package center.bedwars.lobby.parkour.model;

import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Parkour {

    private final String id;
    private final Location startLocation;
    private final List<ParkourCheckpoint> checkpoints;
    private final Location finishLocation;

    @Setter
    private Hologram startHologram;
    private final Map<Integer, Hologram> checkpointHolograms = new HashMap<>();
    @Setter
    private Hologram finishHologram;

    public Parkour(String id, Location startLocation, List<ParkourCheckpoint> checkpoints,
                   Location finishLocation) {
        this.id = id;
        this.startLocation = startLocation;
        this.checkpoints = checkpoints;
        this.finishLocation = finishLocation;
    }

    public void addCheckpointHologram(int checkpointNumber, Hologram hologram) {
        checkpointHolograms.put(checkpointNumber, hologram);
    }

    public ParkourCheckpoint getCheckpointAt(Location location) {
        for (ParkourCheckpoint checkpoint : checkpoints) {
            if (checkpoint.location().getBlock().getLocation()
                    .equals(location.getBlock().getLocation())) {
                return checkpoint;
            }
        }
        return null;
    }

    public ParkourCheckpoint getCheckpoint(int number) {
        for (ParkourCheckpoint checkpoint : checkpoints) {
            if (checkpoint.number() == number) return checkpoint;
        }
        return null;
    }
}
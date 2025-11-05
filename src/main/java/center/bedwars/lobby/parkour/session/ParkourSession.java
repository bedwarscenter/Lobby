package center.bedwars.lobby.parkour.session;

import center.bedwars.lobby.parkour.model.Parkour;
import center.bedwars.lobby.parkour.model.ParkourCheckpoint;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

@Getter
public class ParkourSession {

    private final Player player;
    private final Parkour parkour;
    private final long startTime;
    private final Set<Integer> reachedCheckpoints;

    public ParkourSession(Player player, Parkour parkour) {
        this.player = player;
        this.parkour = parkour;
        this.startTime = System.currentTimeMillis();
        this.reachedCheckpoints = new HashSet<>();
    }

    public void reachCheckpoint(int checkpointNumber) {
        reachedCheckpoints.add(checkpointNumber);
    }

    public boolean hasReachedCheckpoint(int checkpointNumber) {
        return reachedCheckpoints.contains(checkpointNumber);
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public Location getLastCheckpointLocation() {
        int maxCheckpoint = -1;
        for (int checkpoint : reachedCheckpoints) {
            if (checkpoint > maxCheckpoint) {
                maxCheckpoint = checkpoint;
            }
        }

        if (maxCheckpoint == -1) {
            return null;
        }

        ParkourCheckpoint checkpoint = parkour.getCheckpoint(maxCheckpoint);
        return checkpoint != null ? checkpoint.getLocation() : null;
    }
}
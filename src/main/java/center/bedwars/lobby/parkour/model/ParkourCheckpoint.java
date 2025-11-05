package center.bedwars.lobby.parkour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;

@Getter
@AllArgsConstructor
public class ParkourCheckpoint {

    private final int number;
    private final Location location;
}
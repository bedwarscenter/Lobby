package center.bedwars.lobby.sync.serialization;

import org.bukkit.Location;

public class HologramData {

    public String hologramId;
    public LocationData location;
    public String[] lines;

    public HologramData() {
    }

    public HologramData(String hologramId, Location loc, String[] lines) {
        this.hologramId = hologramId;
        this.location = new LocationData(loc);
        this.lines = lines;
    }
}

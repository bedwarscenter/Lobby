package center.bedwars.lobby.sync.serialization;

import org.bukkit.Location;
import org.bukkit.Material;

public class BlockData {

    public LocationData location;
    public String material;
    public byte data;

    public BlockData() {
    }

    public BlockData(Location loc, Material mat, byte data) {
        this.location = new LocationData(loc);
        this.material = mat.name();
        this.data = data;
    }

    public Material getMaterial() {
        return Material.getMaterial(material);
    }
}

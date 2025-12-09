package center.bedwars.lobby.sync.serialization;

import org.bukkit.Location;

public class NPCData {

    public short npcId;
    public String name;
    public LocationData location;
    public String texture;
    public String signature;

    public NPCData() {
    }

    public NPCData(String npcId, String name, Location loc, String texture, String signature) {
        try {
            this.npcId = Short.parseShort(npcId);
        } catch (NumberFormatException e) {
            this.npcId = (short) npcId.hashCode();
        }
        this.name = name;
        this.location = new LocationData(loc);
        this.texture = texture != null ? texture : "";
        this.signature = signature != null ? signature : "";
    }
}

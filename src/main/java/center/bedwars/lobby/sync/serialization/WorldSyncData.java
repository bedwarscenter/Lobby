package center.bedwars.lobby.sync.serialization;

import java.util.Map;

public class WorldSyncData {

    public String worldName;
    public WorldBorderData border;
    public Map<String, String> gameRules;
    public String difficulty;
    public boolean pvp;
    public long time;
    public boolean storm;
    public boolean thundering;

    public WorldSyncData() {
    }
}

package center.bedwars.lobby.parkour.session;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParkourSessionManager {

    private final Map<UUID, ParkourSession> sessions = new HashMap<>();

    public void addSession(Player player, ParkourSession session) {
        sessions.put(player.getUniqueId(), session);
    }

    public ParkourSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean hasActiveSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void endSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public void clearAllSessions() {
        sessions.clear();
    }
}
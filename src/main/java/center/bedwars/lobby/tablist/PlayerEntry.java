package center.bedwars.lobby.tablist;

import org.bukkit.entity.Player;
import xyz.refinedev.phoenix.rank.IRank;

public class PlayerEntry {

    private final Player player;
    private final String displayName;
    private final int priority;
    private final IRank rank;

    public PlayerEntry(Player player, String displayName, int priority, IRank rank) {
        this.player = player;
        this.displayName = displayName;
        this.priority = priority;
        this.rank = rank;
    }

    public Player getPlayer() {
        return player;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriority() {
        return priority;
    }

    public IRank getRank() {
        return rank;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlayerEntry)) return false;

        PlayerEntry other = (PlayerEntry) obj;
        return player.getUniqueId().equals(other.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }

    @Override
    public String toString() {
        return "PlayerEntry{" +
                "player=" + player.getName() +
                ", priority=" + priority +
                ", rank=" + (rank != null ? rank.getName() : "none") +
                '}';
    }
}
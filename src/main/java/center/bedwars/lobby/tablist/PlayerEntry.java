package center.bedwars.lobby.tablist;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.refinedev.phoenix.rank.IRank;

public record PlayerEntry(Player player, String displayName, int priority, IRank rank) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlayerEntry other)) return false;

        return player.getUniqueId().equals(other.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return "PlayerEntry{" +
                "player=" + player.getName() +
                ", priority=" + priority +
                ", rank=" + (rank != null ? rank.getName() : "none") +
                '}';
    }
}
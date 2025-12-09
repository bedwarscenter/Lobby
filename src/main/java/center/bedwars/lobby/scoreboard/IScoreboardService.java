package center.bedwars.lobby.scoreboard;

import center.bedwars.lobby.service.IService;
import org.bukkit.entity.Player;

public interface IScoreboardService extends IService {
    void createScoreboard(Player player);

    void removeScoreboard(Player player);

    void updateScoreboard(Player player);

    void reload();
}

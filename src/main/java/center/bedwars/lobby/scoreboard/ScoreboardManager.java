package center.bedwars.lobby.scoreboard;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.ScoreboardConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.util.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ScoreboardManager extends Manager {

    private final Map<UUID, PlayerScoreboard> scoreboards = new ConcurrentHashMap<>();
    private final TitleAnimator titleAnimator = new TitleAnimator();
    private final ScoreboardFormatter formatter = new ScoreboardFormatter();
    private BukkitTask updateTask;

    @Override
    protected void onLoad() {
        startUpdateTask();
    }

    @Override
    protected void onUnload() {
        stopUpdateTask();
        removeAllScoreboards();
    }

    public void reload() {
        stopUpdateTask();
        titleAnimator.reset();
        startUpdateTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
            createScoreboard(player);
        }
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Lobby.getINSTANCE(),
                this::updateAllScoreboards,
                0L,
                ScoreboardConfiguration.TITLE_ANIMATION_SPEED
        );
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void removeAllScoreboards() {
        scoreboards.values().forEach(PlayerScoreboard::remove);
        scoreboards.clear();
    }

    private void updateAllScoreboards() {
        titleAnimator.nextFrame();
        Bukkit.getOnlinePlayers().forEach(this::updateScoreboard);
    }

    public void createScoreboard(Player player) {
        scoreboards.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerScoreboard scoreboard = new PlayerScoreboard(player);
            scoreboard.create();
            updateScoreboard(player);
            return scoreboard;
        });
    }

    public void removeScoreboard(Player player) {
        PlayerScoreboard scoreboard = scoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            scoreboard.remove();
        }
    }

    private void updateScoreboard(Player player) {
        PlayerScoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        String title = formatter.formatTitle(player, titleAnimator.getCurrentFrame());
        List<String> lines = formatter.formatLines(player);

        scoreboard.update(title, lines);
    }

    private static class TitleAnimator {
        private int currentFrame = 0;

        public void nextFrame() {
            currentFrame++;
            if (currentFrame >= getTotalFrames()) {
                currentFrame = 0;
            }
        }

        public void reset() {
            currentFrame = 0;
        }

        public int getCurrentFrame() {
            return currentFrame;
        }

        private int getTotalFrames() {
            return Math.max(1, ScoreboardConfiguration.TITLE_FRAMES.size());
        }
    }

    private class ScoreboardFormatter {

        public String formatTitle(Player player, int frame) {
            if (ScoreboardConfiguration.TITLE_FRAMES.isEmpty()) {
                return ColorUtil.color("&f&lBED WARS");
            }

            int safeFrame = frame % ScoreboardConfiguration.TITLE_FRAMES.size();
            String title = ScoreboardConfiguration.TITLE_FRAMES.get(safeFrame);

            return ColorUtil.color(parsePlaceholders(player, title));
        }

        public List<String> formatLines(Player player) {
            return ScoreboardConfiguration.LINES.stream()
                    .map(line -> parsePlaceholders(player, line))
                    .map(ColorUtil::color)
                    .collect(Collectors.toList());
        }
    }

    private String parsePlaceholders(Player player, String text) {
        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        if (dependencyManager == null) return text;

        PlaceholderAPIDependency placeholderAPI = dependencyManager.getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isApiAvailable()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}
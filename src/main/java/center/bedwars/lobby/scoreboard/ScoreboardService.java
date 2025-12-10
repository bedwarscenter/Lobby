package center.bedwars.lobby.scoreboard;

import center.bedwars.api.scoreboard.Scoreboard;
import center.bedwars.api.util.ColorUtil;
import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.ScoreboardConfiguration;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency;
import center.bedwars.lobby.service.AbstractService;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class ScoreboardService extends AbstractService implements IScoreboardService {

    private final Lobby plugin;
    private final IDependencyService dependencyService;
    private final Map<UUID, Scoreboard> scoreboards = new ConcurrentHashMap<>();
    private final TitleAnimator titleAnimator = new TitleAnimator();
    private BukkitTask updateTask;

    @Inject
    public ScoreboardService(Lobby plugin, IDependencyService dependencyService) {
        this.plugin = plugin;
        this.dependencyService = dependencyService;
    }

    @Override
    protected void onEnable() {
        startUpdateTask();
    }

    @Override
    protected void onDisable() {
        stopUpdateTask();
        removeAllScoreboards();
    }

    @Override
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
                plugin,
                this::updateAllScoreboards,
                0L,
                ScoreboardConfiguration.TITLE_ANIMATION_SPEED);
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void removeAllScoreboards() {
        scoreboards.values().forEach(Scoreboard::remove);
        scoreboards.clear();
    }

    private void updateAllScoreboards() {
        titleAnimator.nextFrame();
        Bukkit.getOnlinePlayers().forEach(this::updateScoreboard);
    }

    @Override
    public void createScoreboard(Player player) {
        scoreboards.computeIfAbsent(player.getUniqueId(), uuid -> {
            Scoreboard scoreboard = new Scoreboard(player);
            Bukkit.getScheduler().runTask(plugin, scoreboard::create);
            updateScoreboard(player);
            return scoreboard;
        });
    }

    @Override
    public void removeScoreboard(Player player) {
        Scoreboard scoreboard = scoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            Bukkit.getScheduler().runTask(plugin, scoreboard::remove);
        }
    }

    @Override
    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null)
            return;

        String title = titleAnimator.getCurrentTitle();
        List<String> lines = formatLines(player);

        Bukkit.getScheduler().runTask(plugin, () -> scoreboard.update(title, lines));
    }

    private List<String> formatLines(Player player) {
        return ScoreboardConfiguration.LINES.stream()
                .map(line -> formatLine(player, line))
                .collect(Collectors.toList());
    }

    private String formatLine(Player player, String line) {
        line = replacePlaceholders(player, line);
        line = ColorUtil.color(line);
        return line;
    }

    private String replacePlaceholders(Player player, String text) {
        text = text.replace("%player%", player.getName());
        text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        PlaceholderAPIDependency papiDep = dependencyService.getPlaceholderAPI();
        if (papiDep != null && papiDep.isApiAvailable()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }

    private static class TitleAnimator {
        private int currentFrame = 0;

        String getCurrentTitle() {
            List<String> frames = ScoreboardConfiguration.TITLE_FRAMES;
            if (frames == null || frames.isEmpty()) {
                return "";
            }
            return ColorUtil.color(frames.get(currentFrame));
        }

        void nextFrame() {
            List<String> frames = ScoreboardConfiguration.TITLE_FRAMES;
            if (frames != null && !frames.isEmpty()) {
                currentFrame = (currentFrame + 1) % frames.size();
            }
        }

        void reset() {
            currentFrame = 0;
        }
    }
}

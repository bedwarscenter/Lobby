package center.bedwars.lobby.scoreboard;

import center.bedwars.lobby.nms.NMSHelper;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class PlayerScoreboard {

    private final Player player;
    private final String objectiveName;
    private final LineCache lineCache;
    private final ObjectivePacketFactory objectiveFactory;
    private final TeamManager teamManager;
    private String currentTitle = "";

    public PlayerScoreboard(Player player) {
        this.player = player;
        this.objectiveName = "bwsb_" + player.getName();
        this.lineCache = new LineCache();
        this.objectiveFactory = new ObjectivePacketFactory();
        ScorePacketFactory scoreFactory = new ScorePacketFactory();
        this.teamManager = new TeamManager(player, scoreFactory);
    }

    public void create() {
        try {
            sendPacket(objectiveFactory.createObjective(objectiveName, "", 0));
            sendPacket(objectiveFactory.createDisplay(1, objectiveName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(String title, List<String> lines) {
        updateTitle(title);
        updateLines(lines);
    }

    private void updateTitle(String title) {
        if (currentTitle.equals(title))
            return;

        try {
            String truncatedTitle = TextTruncator.truncate(title, 32);
            sendPacket(objectiveFactory.createObjective(objectiveName, truncatedTitle, 2));
            currentTitle = title;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLines(List<String> lines) {
        removeExtraLines(lines.size());
        updateExistingLines(lines);
    }

    private void removeExtraLines(int newSize) {
        for (int i = 15; i > newSize; i--) {
            if (lineCache.hasLine(i)) {
                teamManager.removeLine(i);
                lineCache.removeLine(i);
            }
        }
    }

    private void updateExistingLines(List<String> lines) {
        int score = lines.size();
        for (String line : lines) {
            updateLine(score, line);
            score--;
        }
    }

    private void updateLine(int score, String line) {
        String truncatedLine = TextTruncator.truncate(line, 40);

        if (lineCache.hasChanged(score, truncatedLine)) {
            teamManager.updateLine(score, truncatedLine);
            lineCache.updateLine(score, truncatedLine);
        }
    }

    public void remove() {
        try {
            teamManager.removeAllLines();
            sendPacket(objectiveFactory.createObjective(objectiveName, "", 1));
            lineCache.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(Packet<?> packet) {
        NMSHelper.sendPacket(player, packet);
    }

    private static class LineCache {
        private final Map<Integer, String> lines = new HashMap<>();

        public boolean hasLine(int score) {
            return lines.containsKey(score);
        }

        public boolean hasChanged(int score, String line) {
            String currentLine = lines.get(score);
            return currentLine == null || !currentLine.equals(line);
        }

        public void updateLine(int score, String line) {
            lines.put(score, line);
        }

        public void removeLine(int score) {
            lines.remove(score);
        }

        public void clear() {
            lines.clear();
        }

        public Set<Integer> getScores() {
            return new HashSet<>(lines.keySet());
        }
    }

    private static class TextTruncator {
        public static String truncate(String text, int maxLength) {
            return text.length() > maxLength ? text.substring(0, maxLength) : text;
        }
    }

    private static class ObjectivePacketFactory {

        public PacketPlayOutScoreboardObjective createObjective(String name, String displayName, int mode) {
            PacketPlayOutScoreboardObjective packet = new PacketPlayOutScoreboardObjective();
            ReflectionUtil.setField(packet, "a", name);
            ReflectionUtil.setField(packet, "b", displayName);
            ReflectionUtil.setField(packet, "c", IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
            ReflectionUtil.setField(packet, "d", mode);
            return packet;
        }

        public PacketPlayOutScoreboardDisplayObjective createDisplay(int position, String name) {
            PacketPlayOutScoreboardDisplayObjective packet = new PacketPlayOutScoreboardDisplayObjective();
            ReflectionUtil.setField(packet, "a", position);
            ReflectionUtil.setField(packet, "b", name);
            return packet;
        }
    }

    private static class ScorePacketFactory {

        public PacketPlayOutScoreboardScore createScore(String entry, String objective, int score,
                PacketPlayOutScoreboardScore.EnumScoreboardAction action) {
            try {
                return createWithConstructor(entry, objective, score, action);
            } catch (Exception e) {
                return createWithFields(entry, objective, score, action);
            }
        }

        private PacketPlayOutScoreboardScore createWithConstructor(String entry, String objective, int score,
                PacketPlayOutScoreboardScore.EnumScoreboardAction action) throws Exception {
            Constructor<PacketPlayOutScoreboardScore> constructor = PacketPlayOutScoreboardScore.class
                    .getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            PacketPlayOutScoreboardScore packet = constructor.newInstance(entry);

            ReflectionUtil.setField(packet, "b", objective);
            ReflectionUtil.setField(packet, "c", score);
            ReflectionUtil.setField(packet, "d", action);

            return packet;
        }

        private PacketPlayOutScoreboardScore createWithFields(String entry, String objective, int score,
                PacketPlayOutScoreboardScore.EnumScoreboardAction action) {
            PacketPlayOutScoreboardScore packet = new PacketPlayOutScoreboardScore();
            ReflectionUtil.setField(packet, "a", entry);
            ReflectionUtil.setField(packet, "b", objective);
            ReflectionUtil.setField(packet, "c", score);
            ReflectionUtil.setField(packet, "d", action);
            return packet;
        }
    }

    private class TeamManager {
        private final Player player;
        private final ScorePacketFactory scoreFactory;
        private final TeamPacketFactory teamFactory;
        private final LineColorHandler colorHandler;

        public TeamManager(Player player, ScorePacketFactory scoreFactory) {
            this.player = player;
            this.scoreFactory = scoreFactory;
            this.teamFactory = new TeamPacketFactory();
            this.colorHandler = new LineColorHandler();
        }

        public void updateLine(int score, String line) {
            String entry = ScoreboardEntry.create(score);

            if (lineCache.hasLine(score)) {
                removeScore(entry, score);
            }

            updateTeam(score, line);
            addScore(entry, score);
        }

        public void removeLine(int score) {
            String entry = ScoreboardEntry.create(score);
            String teamName = TeamNameGenerator.generate(score);

            removeScore(entry, score);
            removeTeam(teamName);
        }

        public void removeAllLines() {
            new ArrayList<>(lineCache.getScores()).forEach(this::removeLine);
        }

        private void updateTeam(int score, String line) {
            String teamName = TeamNameGenerator.generate(score);
            String entry = ScoreboardEntry.create(score);

            removeTeam(teamName);
            createTeam(teamName, entry, line);
        }

        private void createTeam(String teamName, String entry, String line) {
            LineSplitter splitter = colorHandler.split(line);
            PacketPlayOutScoreboardTeam packet = teamFactory.createTeam(teamName, entry, splitter);
            sendPacket(packet);
        }

        private void removeTeam(String teamName) {
            PacketPlayOutScoreboardTeam packet = teamFactory.removeTeam(teamName);
            sendPacket(packet);
        }

        private void addScore(String entry, int score) {
            try {
                PacketPlayOutScoreboardScore packet = scoreFactory.createScore(
                        entry, objectiveName, score,
                        PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE);
                sendPacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void removeScore(String entry, int score) {
            try {
                PacketPlayOutScoreboardScore packet = scoreFactory.createScore(
                        entry, objectiveName, score,
                        PacketPlayOutScoreboardScore.EnumScoreboardAction.REMOVE);
                sendPacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendPacket(Packet<?> packet) {
            NMSHelper.sendPacket(player, packet);
        }
    }

    private static class TeamPacketFactory {

        public PacketPlayOutScoreboardTeam createTeam(String teamName, String entry, LineSplitter splitter) {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            ReflectionUtil.setField(packet, "a", teamName);
            ReflectionUtil.setField(packet, "b", "");
            ReflectionUtil.setField(packet, "c", splitter.prefix());
            ReflectionUtil.setField(packet, "d", splitter.suffix());
            ReflectionUtil.setField(packet, "e", "always");
            ReflectionUtil.setField(packet, "h", 0);
            ReflectionUtil.setField(packet, "g", Collections.singletonList(entry));
            return packet;
        }

        public PacketPlayOutScoreboardTeam removeTeam(String teamName) {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
            ReflectionUtil.setField(packet, "a", teamName);
            ReflectionUtil.setField(packet, "h", 1);
            return packet;
        }
    }

    private static class LineColorHandler {

        public LineSplitter split(String line) {
            if (line.length() <= 16) {
                return new LineSplitter(line, "");
            }

            String prefix = line.substring(0, 16);
            String suffix = line.substring(16);

            String lastColor = extractLastColor(prefix);
            if (!lastColor.isEmpty() && !suffix.startsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
                suffix = lastColor + suffix;
            }

            suffix = TextTruncator.truncate(suffix, 16);

            return new LineSplitter(prefix, suffix);
        }

        private String extractLastColor(String text) {
            String lastColor = "";
            for (int i = 0; i < text.length() - 1; i++) {
                if (text.charAt(i) == ChatColor.COLOR_CHAR) {
                    char code = text.charAt(i + 1);
                    if (isValidColorCode(code)) {
                        lastColor = String.valueOf(ChatColor.COLOR_CHAR) + code;
                    }
                }
            }
            return lastColor;
        }

        private boolean isValidColorCode(char code) {
            return "0123456789abcdefklmnor".indexOf(Character.toLowerCase(code)) != -1;
        }
    }

    private record LineSplitter(String prefix, String suffix) {
    }

    private static class ScoreboardEntry {
        public static String create(int score) {
            char colorChar = "0123456789abcdef".charAt(score % 16);
            if (score >= 16) {
                char secondChar = "0123456789abcdef".charAt(score / 16 % 16);
                return "" + ChatColor.COLOR_CHAR + secondChar + ChatColor.COLOR_CHAR + colorChar + ChatColor.RESET;
            }
            return "" + ChatColor.COLOR_CHAR + colorChar + ChatColor.RESET;
        }
    }

    private static class TeamNameGenerator {
        public static String generate(int score) {
            return "sb_team_" + score;
        }
    }

    private static class ReflectionUtil {
        public static void setField(Object object, String fieldName, Object value) {
            try {
                Field field = object.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
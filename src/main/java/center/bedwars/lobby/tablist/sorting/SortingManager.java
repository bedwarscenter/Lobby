package center.bedwars.lobby.tablist.sorting;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.TablistConfiguration;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.PlaceholderAPIDependency;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SortingManager {

    private final List<SortingType> sortingTypes = new ArrayList<>();

    public void reload() {
        sortingTypes.clear();
        parseSortingConfig();
    }

    private void parseSortingConfig() {
        for (String sortType : TablistConfiguration.SORTING_TYPES) {
            SortingType parsed = parseSortingType(sortType);
            if (parsed != null) {
                sortingTypes.add(parsed);
            }
        }
    }

    private SortingType parseSortingType(String config) {
        if (config.startsWith("GROUPS:")) {
            return new GroupsSortingType(config.substring(7));
        } else if (config.startsWith("PLACEHOLDER_A_TO_Z:")) {
            return new PlaceholderAToZSortingType(config.substring(19));
        } else if (config.startsWith("PLACEHOLDER_Z_TO_A:")) {
            return new PlaceholderZToASortingType(config.substring(19));
        } else if (config.startsWith("PLACEHOLDER_LOW_TO_HIGH:")) {
            return new PlaceholderLowToHighSortingType(config.substring(24));
        } else if (config.startsWith("PLACEHOLDER_HIGH_TO_LOW:")) {
            return new PlaceholderHighToLowSortingType(config.substring(24));
        } else if (config.startsWith("PLACEHOLDER:")) {
            return new PlaceholderValuesSortingType(config.substring(12));
        }
        return null;
    }

    public int compare(Player a, Player b, String rankA, String rankB) {
        for (SortingType sortingType : sortingTypes) {
            int comparison = sortingType.compare(a, b, rankA, rankB);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private interface SortingType {
        int compare(Player a, Player b, String rankA, String rankB);
    }

    private static class GroupsSortingType implements SortingType {
        private final List<List<String>> groupPriorities = new ArrayList<>();

        public GroupsSortingType(String config) {
            String[] groups = config.split(",");
            for (String group : groups) {
                List<String> samePriorityGroups = new ArrayList<>();
                if (group.contains("|")) {
                    for (String g : group.split("\\|")) {
                        samePriorityGroups.add(g.trim());
                    }
                } else {
                    samePriorityGroups.add(group.trim());
                }
                groupPriorities.add(samePriorityGroups);
            }
        }

        @Override
        public int compare(Player a, Player b, String rankA, String rankB) {
            return Integer.compare(getPriority(rankA), getPriority(rankB));
        }

        private int getPriority(String rankName) {
            for (int i = 0; i < groupPriorities.size(); i++) {
                if (groupPriorities.get(i).contains(rankName)) {
                    return i;
                }
            }
            return groupPriorities.size();
        }
    }

    private class PlaceholderAToZSortingType implements SortingType {
        private final String placeholder;

        public PlaceholderAToZSortingType(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public int compare(Player a, Player b, String rankA, String rankB) {
            String valueA = parsePlaceholder(a, placeholder);
            String valueB = parsePlaceholder(b, placeholder);

            if (TablistConfiguration.CASE_SENSITIVE_SORTING) {
                return valueA.compareTo(valueB);
            }
            return valueA.compareToIgnoreCase(valueB);
        }
    }

    private class PlaceholderZToASortingType implements SortingType {
        private final String placeholder;

        public PlaceholderZToASortingType(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public int compare(Player a, Player b, String rankA, String rankB) {
            String valueA = parsePlaceholder(a, placeholder);
            String valueB = parsePlaceholder(b, placeholder);

            if (TablistConfiguration.CASE_SENSITIVE_SORTING) {
                return valueB.compareTo(valueA);
            }
            return valueB.compareToIgnoreCase(valueA);
        }
    }

    private class PlaceholderLowToHighSortingType implements SortingType {
        private final String placeholder;

        public PlaceholderLowToHighSortingType(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public int compare(Player a, Player b, String rankA, String rankB) {
            double valueA = parseNumber(parsePlaceholder(a, placeholder));
            double valueB = parseNumber(parsePlaceholder(b, placeholder));
            return Double.compare(valueA, valueB);
        }

        private double parseNumber(String value) {
            try {
                return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private class PlaceholderHighToLowSortingType implements SortingType {
        private final String placeholder;

        public PlaceholderHighToLowSortingType(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        public int compare(Player a, Player b, String rankA, String rankB) {
            double valueA = parseNumber(parsePlaceholder(a, placeholder));
            double valueB = parseNumber(parsePlaceholder(b, placeholder));
            return Double.compare(valueB, valueA);
        }

        private double parseNumber(String value) {
            try {
                return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private class PlaceholderValuesSortingType implements SortingType {
        private final String placeholder;
        private final List<List<String>> valuePriorities = new ArrayList<>();

        public PlaceholderValuesSortingType(String config) {
            String[] parts = config.split(":", 2);
            this.placeholder = parts[0];

            if (parts.length > 1) {
                String[] values = parts[1].split(",");
                for (String value : values) {
                    List<String> samePriorityValues = new ArrayList<>();
                    if (value.contains("|")) {
                        for (String v : value.split("\\|")) {
                            samePriorityValues.add(v.trim());
                        }
                    } else {
                        samePriorityValues.add(value.trim());
                    }
                    valuePriorities.add(samePriorityValues);
                }
            }
        }

        @Override
        public int compare(Player a, Player b, String rankA, String rankB) {
            String valueA = parsePlaceholder(a, placeholder);
            String valueB = parsePlaceholder(b, placeholder);
            return Integer.compare(getPriority(valueA), getPriority(valueB));
        }

        private int getPriority(String value) {
            for (int i = 0; i < valuePriorities.size(); i++) {
                if (valuePriorities.get(i).contains(value)) {
                    return i;
                }
            }
            return valuePriorities.size();
        }
    }

    private String parsePlaceholder(Player player, String text) {
        if (text == null) return "";

        text = text.replace("%player_name%", player.getName())
                .replace("%player%", player.getName());

        DependencyManager dependencyManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        if (dependencyManager == null) return text;

        PlaceholderAPIDependency placeholderAPI = dependencyManager.getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isApiAvailable()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }
}
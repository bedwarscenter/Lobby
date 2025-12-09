package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.snow.ISnowService;
import com.google.inject.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleSnowCommand implements CommandExecutor {

    private final ISnowService snowService;

    @Inject
    public ToggleSnowCommand(ISnowService snowService) {
        this.snowService = snowService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!SettingsConfiguration.SNOW_RAIN.ENABLED) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    SettingsConfiguration.SNOW_RAIN.MESSAGES.FEATURE_DISABLED));
            return true;
        }

        snowService.toggleSnow(player);
        return true;
    }
}

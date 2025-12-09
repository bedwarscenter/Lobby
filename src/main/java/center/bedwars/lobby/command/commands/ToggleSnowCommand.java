package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.snow.ISnowService;
import center.bedwars.lobby.util.ColorUtil;
import com.google.inject.Inject;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "togglesnow", aliases = { "snow", "ts" })
@SuppressWarnings("unused")
public class ToggleSnowCommand {

    private final ISnowService snowService;

    @Inject
    public ToggleSnowCommand(ISnowService snowService) {
        this.snowService = snowService;
    }

    @Command(name = "")
    public void toggle(@Sender Player sender) {
        if (!SettingsConfiguration.SNOW_RAIN.ENABLED) {
            ColorUtil.sendMessage(sender, SettingsConfiguration.SNOW_RAIN.MESSAGES.FEATURE_DISABLED);
            return;
        }

        snowService.toggleSnow(sender);
    }
}

package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.api.util.ColorUtil;
import com.google.inject.Inject;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "parkour")
@SuppressWarnings("unused")
public class ParkourCommand {

    private final IParkourService parkourService;

    @Inject
    public ParkourCommand(IParkourService parkourService) {
        this.parkourService = parkourService;
    }

    @Command(name = "")
    public void parkour(@Sender Player player) {
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.TITLE);
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.CHECKPOINT_HELP);
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.RESET_HELP);
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.QUIT_HELP);
    }

    @Command(name = "checkpoint")
    public void checkpoint(@Sender Player player) {
        parkourService.teleportToCheckpoint(player);
    }

    @Command(name = "reset")
    public void reset(@Sender Player player) {
        parkourService.resetPlayer(player);
    }

    @Command(name = "quit")
    public void quit(@Sender Player player) {
        parkourService.quitParkour(player);
    }
}

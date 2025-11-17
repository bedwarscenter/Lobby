package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.util.ColorUtil;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "parkour")
public class ParkourCommand {

    private final ParkourManager parkourManager;

    public ParkourCommand() {
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
    }

    @Command(name = "")
    public void main(@Sender Player player) {
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.TITLE);
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.CHECKPOINT_HELP);
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.RESET_HELP);
        ColorUtil.sendMessage(player, LanguageConfiguration.COMMAND.PARKOUR_COMMAND.QUIT_HELP);
    }

    @Command(name = "checkpoint")
    public void checkpoint(@Sender Player player) {
        parkourManager.teleportToCheckpoint(player);
    }

    @Command(name = "reset")
    public void reset(@Sender Player player) {
        parkourManager.resetPlayer(player);
    }

    @Command(name = "quit")
    public void quit(@Sender Player player) {
        parkourManager.quitParkour(player);
    }
}
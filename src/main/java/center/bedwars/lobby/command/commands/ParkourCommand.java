package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.util.ColorUtil;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "parkour")
@SuppressWarnings("unused")
public class ParkourCommand {

    private final ParkourManager parkourManager;

    public ParkourCommand() {
        this.parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
    }

    @Command(name = "")
    public void main(@Sender Player player) {
        ColorUtil.sendMessage(player, "&6&lPARKOUR COMMANDS");
        ColorUtil.sendMessage(player, "&e/parkour checkpoint &7- Teleport to last checkpoint");
        ColorUtil.sendMessage(player, "&e/parkour reset &7- Reset and quit the parkour");
        ColorUtil.sendMessage(player, "&e/parkour quit &7- Quit the parkour");
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
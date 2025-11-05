package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.util.ChatColor;
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
        player.sendMessage(ChatColor.translate("&6&lPARKOUR COMMANDS"));
        player.sendMessage(ChatColor.translate("&e/parkour checkpoint &7- Teleport to last checkpoint"));
        player.sendMessage(ChatColor.translate("&e/parkour reset &7- Reset and quit the parkour"));
        player.sendMessage(ChatColor.translate("&e/parkour quit &7- Quit the parkour"));
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
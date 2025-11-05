package center.bedwars.lobby.command.commands;

import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.command.Requires;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.command.CommandSender;

@Register(name = "bedwarslobby", aliases = {"bwl"})
@Requires("bedwarslobby.command.admin")
@SuppressWarnings("unused")
public class BedWarsLobbyCommand {

    @Command(name = "")
    public void main(@Sender CommandSender sender) {

    }
}

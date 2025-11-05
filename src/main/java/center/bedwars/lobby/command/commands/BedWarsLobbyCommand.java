package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.util.ColorUtil;
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
        ColorUtil.sendMessage(sender, "&6&lBedWarsLobby &7- Admin Commands");
        ColorUtil.sendMessage(sender, "&e/bwl reload &7- Reload plugin");
        ColorUtil.sendMessage(sender, "&e/bwl sync &7- Force synchronization");
    }
}
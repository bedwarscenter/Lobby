package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.util.ColorUtil;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.command.Requires;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Register(name = "bedwarslobby", aliases = {"bwl"})
@Requires("bedwarslobby.command.admin")
@SuppressWarnings("unused")
public class BedWarsLobbyCommand {

    private final Lobby lobby = Lobby.getINSTANCE();

    @Command(name = "")
    public void main(@Sender Player sender) {
        ColorUtil.sendMessage(sender, "&6&lBedWarsLobby &7- Admin Commands");
        ColorUtil.sendMessage(sender, "&e/bwl reload &7- Reload configurations");
        ColorUtil.sendMessage(sender, "&e/bwl parkour &7- Reload parkours");
    }

    @Command(name = "reload")
    public void reload(@Sender Player sender) {
        ColorUtil.sendMessage(sender, "&aReloading configurations...");

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            long startTime = System.currentTimeMillis();
            ConfigurationManager.reloadConfigurations();
            long reloadTime = System.currentTimeMillis() - startTime;

            Bukkit.getScheduler().runTask(lobby, () -> {
                ColorUtil.sendMessage(sender, "&aConfigurations reloaded! &7(" + reloadTime + "ms)");
            });
        });
    }

    @Command(name = "parkour")
    public void parkour(@Sender Player sender) {
        ColorUtil.sendMessage(sender, "&aScanning for parkours...");

        Bukkit.getScheduler().runTask(lobby, () -> {
            try {
                ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
                parkourManager.refreshParkours();
                ColorUtil.sendMessage(sender, "&aParkours refreshed!");
            } catch (Exception e) {
                ColorUtil.sendMessage(sender, "&cFailed to refresh parkours: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
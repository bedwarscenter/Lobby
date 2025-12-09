package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import com.google.inject.Inject;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "spawn", aliases = { "stuck" })
@SuppressWarnings("unused")
public class SpawnCommand {

    private final IParkourService parkourService;

    @Inject
    public SpawnCommand(IParkourService parkourService) {
        this.parkourService = parkourService;
    }

    @Command(name = "")
    public void teleport(@Sender Player sender) {
        if (parkourService != null && parkourService.hasActiveSession(sender)) {
            parkourService.leaveParkour(sender, true);
        }

        boolean success = SpawnUtil.teleportToSpawn(sender);
        if (success) {
            ColorUtil.sendMessage(sender, LanguageConfiguration.SPAWN.TELEPORTED);
        } else {
            ColorUtil.sendMessage(sender, LanguageConfiguration.SPAWN.NOT_CONFIGURED);
        }
    }
}

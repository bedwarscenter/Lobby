package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.util.ColorUtil;
import center.bedwars.lobby.util.SpawnUtil;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "spawn", aliases = {"stuck"})
@SuppressWarnings("unused")
public class SpawnCommand {

    @Command(name = "")
    public void teleport(@Sender Player sender) {
        ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
        if (parkourManager != null && parkourManager.getSessionManager().hasActiveSession(sender)) {
            parkourManager.leaveParkour(sender, true);
        }

        boolean success = SpawnUtil.teleportToSpawn(sender);
        if (success) {
            ColorUtil.sendMessage(sender, LanguageConfiguration.SPAWN.TELEPORTED);
        } else {
            ColorUtil.sendMessage(sender, LanguageConfiguration.SPAWN.NOT_CONFIGURED);
        }
    }
}


package center.bedwars.lobby.command.commands;

import com.yapzhenyie.GadgetsMenu.api.GadgetsMenuAPI;
import com.yapzhenyie.GadgetsMenu.player.PlayerManager;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

@Register(name = "collectibles", aliases = {"gadgets"})
@SuppressWarnings("unused")
public class CollectiblesCommand {

    @Command(name = "")
    public void openCollectibles(@Sender Player sender) {
        PlayerManager playerManager = GadgetsMenuAPI.getPlayerManager(sender);
        playerManager.goBackToMainMenu();
    }
}

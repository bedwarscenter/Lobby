package center.bedwars.lobby.command.commands;

import de.marcely.bedwars.api.cosmetics.CosmeticsAPI;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;


@Register(name = "cosmetics", aliases = {"shop"})
@SuppressWarnings("unused")
public class CosmeticsCommand {

    @Command(name = "")
    public void openCosmetics(@Sender Player sender) {
        CosmeticsAPI.get().getShopById("main").open(sender);
    }
}
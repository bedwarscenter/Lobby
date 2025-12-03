package center.bedwars.lobby.command.commands;

import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

@Register(name = "cosmetics", aliases = {"shop"})
@SuppressWarnings("unused")
public class CosmeticsCommand {

    @Command(name = "")
    public void openCosmetics(@Sender Player sender) {
        try {
            Class<?> cosmeticsAPIClass = Class.forName("de.marcely.bedwars.api.cosmetics.CosmeticsAPI");

            Method getMethod = cosmeticsAPIClass.getMethod("get");
            Object cosmeticsAPI = getMethod.invoke(null);

            Method getShopByIdMethod = cosmeticsAPIClass.getMethod("getShopById", String.class);
            Object shop = getShopByIdMethod.invoke(cosmeticsAPI, "main");

            Class<?> shopClass = Class.forName("de.marcely.bedwars.api.cosmetics.shop.Shop");
            Method openMethod = shopClass.getMethod("open", Player.class);
            openMethod.invoke(shop, sender);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
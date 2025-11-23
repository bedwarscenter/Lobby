package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.ConfigurationManager;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.parkour.ParkourManager;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.util.ColorUtil;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.command.Requires;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Register(name = "bedwarslobby", aliases = {"bwl"})
@Requires("bedwarslobby.command.admin")
@SuppressWarnings("unused")
public class BedWarsLobbyCommand {

    private final Lobby lobby = Lobby.getINSTANCE();

    @Command(name = "")
    public void bedwarslobby(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.TITLE);
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.RELOAD_HELP);
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.PARKOUR_HELP);
        ColorUtil.sendMessage(sender, "&e/bwl setspawn &7- Set spawn location");
    }

    @Command(name = "reload")
    public void reload(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.RELOADING);

        Bukkit.getScheduler().runTaskAsynchronously(lobby, () -> {
            long startTime = System.currentTimeMillis();
            ConfigurationManager.reloadConfigurations();
            long reloadTime = System.currentTimeMillis() - startTime;

            Bukkit.getScheduler().runTask(lobby, () -> ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.RELOADED
                    .replace("%time%", String.valueOf(reloadTime))));
        });
    }

    @Command(name = "parkour")
    public void parkour(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.PARKOUR_SCANNING);

        Bukkit.getScheduler().runTask(lobby, () -> {
            try {
                ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
                parkourManager.refreshParkours();
                ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.PARKOUR_REFRESHED);
            } catch (Exception e) {
                ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.PARKOUR_FAILED
                        .replace("%error%", e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    @Command(name = "setspawn")
    public void setSpawn(@Sender Player sender) {
        ParkourManager parkourManager = Lobby.getManagerStorage().getManager(ParkourManager.class);
        ParkourSession session = parkourManager.getSessionManager().getSession(sender);

        Location loc;

        loc = sender.getLocation();
        SettingsConfiguration.SPAWN_LOCATION = String.format("%s;%s;%s;%s;%s;%s",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld().getName(),
                loc.getYaw(), loc.getPitch());
        if (session != null) {
            ColorUtil.sendMessage(sender, "&aSet parkour spawn location!");
        } else {

            ColorUtil.sendMessage(sender, "&aSet lobby spawn location!");
        }

        Bukkit.getScheduler().runTaskAsynchronously(lobby, ConfigurationManager::saveConfigurations);
    }
}
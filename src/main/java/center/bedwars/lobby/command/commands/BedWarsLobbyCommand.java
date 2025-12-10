package center.bedwars.lobby.command.commands;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.IConfigurationService;
import center.bedwars.lobby.configuration.configurations.LanguageConfiguration;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.nametag.INametagService;
import center.bedwars.lobby.parkour.IParkourService;
import center.bedwars.lobby.parkour.session.ParkourSession;
import center.bedwars.lobby.scoreboard.IScoreboardService;
import center.bedwars.lobby.tablist.ITablistService;
import center.bedwars.api.util.ColorUtil;
import com.google.inject.Inject;
import net.j4c0b3y.api.command.annotation.command.Command;
import net.j4c0b3y.api.command.annotation.command.Requires;
import net.j4c0b3y.api.command.annotation.parameter.classifier.Sender;
import net.j4c0b3y.api.command.annotation.registration.Register;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Register(name = "bedwarslobby", aliases = { "bwl" })
@Requires("bedwarslobby.command.admin")
@SuppressWarnings("unused")
public class BedWarsLobbyCommand {

    private final Lobby lobby;
    private final IConfigurationService configService;
    private final IScoreboardService scoreboardService;
    private final ITablistService tablistService;
    private final INametagService nametagService;
    private final IParkourService parkourService;

    @Inject
    public BedWarsLobbyCommand(Lobby lobby, IConfigurationService configService,
            IScoreboardService scoreboardService, ITablistService tablistService,
            INametagService nametagService, IParkourService parkourService) {
        this.lobby = lobby;
        this.configService = configService;
        this.scoreboardService = scoreboardService;
        this.tablistService = tablistService;
        this.nametagService = nametagService;
        this.parkourService = parkourService;
    }

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
            configService.reloadConfigurations();
            long reloadTime = System.currentTimeMillis() - startTime;

            Bukkit.getScheduler().runTask(lobby, () -> {
                if (scoreboardService != null) {
                    scoreboardService.reload();
                }

                if (tablistService != null) {
                    tablistService.reload();
                }

                if (nametagService != null) {
                    nametagService.reload();
                }

                ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.RELOADED
                        .replace("%time%", String.valueOf(reloadTime)));
            });
        });
    }

    @Command(name = "parkour")
    public void parkour(@Sender Player sender) {
        ColorUtil.sendMessage(sender, LanguageConfiguration.COMMAND.ADMIN_COMMAND.PARKOUR_SCANNING);

        Bukkit.getScheduler().runTask(lobby, () -> {
            try {
                parkourService.refreshParkours();
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
        ParkourSession session = parkourService.getSession(sender);

        Location loc = sender.getLocation();
        SettingsConfiguration.SPAWN_LOCATION = String.format("%s;%s;%s;%s;%s;%s",
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getWorld().getName(),
                loc.getYaw(), loc.getPitch());

        if (session != null) {
            ColorUtil.sendMessage(sender, "&aSet parkour spawn location!");
        } else {
            ColorUtil.sendMessage(sender, "&aSet lobby spawn location!");
        }

        Bukkit.getScheduler().runTaskAsynchronously(lobby, configService::saveConfigurations);
    }
}

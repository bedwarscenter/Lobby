package center.bedwars.lobby.command;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.command.commands.BedWarsLobbyCommand;
import center.bedwars.lobby.command.commands.ParkourCommand;
import center.bedwars.lobby.command.commands.SyncCommand;
import center.bedwars.lobby.command.commands.SpawnCommand;
import center.bedwars.lobby.manager.Manager;
import lombok.Getter;
import net.j4c0b3y.api.command.bukkit.BukkitCommandHandler;

@Getter
public class CommandManager extends Manager {

    private BukkitCommandHandler bukkitCommandHandler;

    @Override
    protected void onLoad() {
        this.bukkitCommandHandler = new BukkitCommandHandler(Lobby.getINSTANCE());

        bukkitCommandHandler.register(new BedWarsLobbyCommand());
        bukkitCommandHandler.register(new ParkourCommand());
        bukkitCommandHandler.register(new SyncCommand());
        bukkitCommandHandler.register(new SpawnCommand());
    }

    @Override
    protected void onUnload() {

    }

}
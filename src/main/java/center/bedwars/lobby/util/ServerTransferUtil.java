package center.bedwars.lobby.util;

import center.bedwars.lobby.Lobby;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

@Singleton
public class ServerTransferUtil {

    private final Lobby plugin;

    @Inject
    public ServerTransferUtil(Lobby plugin) {
        this.plugin = plugin;
    }

    public void sendToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}

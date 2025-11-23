package center.bedwars.lobby.database;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.database.databases.MongoDatabaseConnection;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import lombok.Getter;

@Getter
public class DatabaseManager extends Manager {

    private final Lobby lobby = Lobby.getINSTANCE();

    private RedisDatabase redis;
    private MongoDatabaseConnection mongo;

    @Override
    protected void onLoad() throws Exception {
        this.redis = new RedisDatabase(lobby.getLogger());
        this.mongo = new MongoDatabaseConnection(lobby.getLogger());

        redis.connect();
        mongo.connect();

        lobby.getLogger().info("Database connections established!");
    }

    @Override
    protected void onUnload() throws Exception {
        if (redis != null) {
            redis.disconnect();
        }

        if (mongo != null) {
            mongo.disconnect();
        }

        lobby.getLogger().info("Database connections closed!");
    }

}
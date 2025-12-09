package center.bedwars.lobby.database;

import center.bedwars.lobby.service.AbstractService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DatabaseService extends AbstractService implements IDatabaseService {

    private final IRedisService redisService;
    private final IMongoService mongoService;

    @Inject
    public DatabaseService(IRedisService redisService, IMongoService mongoService) {
        this.redisService = redisService;
        this.mongoService = mongoService;
    }

    @Override
    protected void onEnable() {
        redisService.enable();
        mongoService.enable();
    }

    @Override
    protected void onDisable() {
        if (redisService != null)
            redisService.disable();
        if (mongoService != null)
            mongoService.disable();
    }

    @Override
    public IRedisService getRedis() {
        return redisService;
    }

    @Override
    public IMongoService getMongo() {
        return mongoService;
    }
}

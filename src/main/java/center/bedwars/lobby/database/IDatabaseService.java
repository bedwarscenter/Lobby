package center.bedwars.lobby.database;

import center.bedwars.lobby.service.IService;

public interface IDatabaseService extends IService {
    IRedisService getRedis();

    IMongoService getMongo();
}

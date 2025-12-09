package center.bedwars.lobby.database;

import center.bedwars.lobby.service.IService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface IRedisService extends IService {
    CompletableFuture<Void> setAsync(String key, String value);

    CompletableFuture<Void> setAsync(String key, String value, int expireSeconds);

    CompletableFuture<String> getAsync(String key);

    CompletableFuture<Void> deleteAsync(String key);

    CompletableFuture<Boolean> existsAsync(String key);

    void publish(String channel, byte[] message);

    void subscribe(String channel, Consumer<byte[]> messageHandler);

    boolean isConnected();
}

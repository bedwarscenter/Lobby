package center.bedwars.lobby.database.databases;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.support.ConnectionPoolSupport;
import lombok.Getter;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
public class RedisDatabase {

    private final Logger logger;
    private RedisClient redisClient;
    private ClientResources clientResources;
    private GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    private final Map<String, StatefulRedisPubSubConnection<byte[], byte[]>> subscriptions = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    public RedisDatabase(Logger logger) {
        this.logger = logger;
    }

    public void connect() {
        try {
            String host = SettingsConfiguration.REDIS.REDIS_HOST;
            int port = SettingsConfiguration.REDIS.REDIS_PORT;
            String password = SettingsConfiguration.REDIS.REDIS_PASSWORD;
            int database = SettingsConfiguration.REDIS.REDIS_DATABASE;
            boolean ssl = SettingsConfiguration.REDIS.REDIS_SSL;

            RedisURI.Builder uriBuilder = RedisURI.Builder
                    .redis(host, port)
                    .withDatabase(database)
                    .withTimeout(Duration.ofSeconds(10));

            if (password != null && !password.isEmpty()) {
                uriBuilder.withPassword(password.toCharArray());
            }

            if (ssl) {
                uriBuilder.withSsl(true);
            }

            RedisURI redisURI = uriBuilder.build();

            this.clientResources = DefaultClientResources.builder()
                    .ioThreadPoolSize(4)
                    .computationThreadPoolSize(4)
                    .commandLatencyCollectorOptions(
                            io.lettuce.core.metrics.CommandLatencyCollectorOptions.disabled()
                    )
                    .build();

            this.redisClient = RedisClient.create(clientResources, redisURI);

            GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(5);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
            poolConfig.setNumTestsPerEvictionRun(3);
            poolConfig.setBlockWhenExhausted(true);

            this.connectionPool = ConnectionPoolSupport.createGenericObjectPool(
                    () -> redisClient.connect(),
                    poolConfig
            );

            this.running = true;

        } catch (Exception e) {
            logger.severe("Failed to connect to Redis: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Redis connection failed", e);
        }
    }

    public void disconnect() {
        this.running = false;

        for (StatefulRedisPubSubConnection<byte[], byte[]> connection : subscriptions.values()) {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        }
        subscriptions.clear();

        if (connectionPool != null && !connectionPool.isClosed()) {
            connectionPool.close();
        }

        if (redisClient != null) {
            redisClient.shutdown();
        }

        if (clientResources != null) {
            clientResources.shutdown();
        }

    }

    public void publish(String channel, byte[] message) {
        if (!running || connectionPool == null) return;

        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection =
                    redisClient.connectPubSub(io.lettuce.core.codec.ByteArrayCodec.INSTANCE);

            try {
                Long receivers = pubSubConnection.sync().publish(
                        channel.getBytes(StandardCharsets.UTF_8),
                        message
                );
            } finally {
                pubSubConnection.close();
            }
        } catch (Exception e) {
            logger.severe("Failed to publish message to Redis channel " + channel + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribe(String channel, Consumer<byte[]> messageHandler) {
        if (!running) return;

        try {
            StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection =
                    redisClient.connectPubSub(io.lettuce.core.codec.ByteArrayCodec.INSTANCE);

            pubSubConnection.addListener(new RedisPubSubAdapter<byte[], byte[]>() {
                @Override
                public void message(byte[] ch, byte[] message) {
                    if (java.util.Arrays.equals(ch, channel.getBytes(StandardCharsets.UTF_8))) {
                        messageHandler.accept(message);
                    }
                }

                @Override
                public void subscribed(byte[] ch, long count) {
                }

                @Override
                public void unsubscribed(byte[] ch, long count) {
                }
            });

            pubSubConnection.async().subscribe(channel.getBytes(StandardCharsets.UTF_8));
            subscriptions.put(channel, pubSubConnection);

        } catch (Exception e) {
            logger.severe("Failed to subscribe to channel " + channel + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void set(String key, String value) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            connection.sync().set(key, value);
        } catch (Exception e) {
            logger.severe("Failed to set key in Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void set(String key, String value, int expireSeconds) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            connection.sync().setex(key, expireSeconds, value);
        } catch (Exception e) {
            logger.severe("Failed to set key with expiration in Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String get(String key) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            return connection.sync().get(key);
        } catch (Exception e) {
            logger.severe("Failed to get key from Redis: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void delete(String key) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            connection.sync().del(key);
        } catch (Exception e) {
            logger.severe("Failed to delete key from Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean exists(String key) {
        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            return connection.sync().exists(key) > 0;
        } catch (Exception e) {
            logger.severe("Failed to check key existence in Redis: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        if (redisClient == null || connectionPool == null || connectionPool.isClosed()) {
            return false;
        }

        try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
            return "PONG".equals(connection.sync().ping());
        } catch (Exception e) {
            return false;
        }
    }
}

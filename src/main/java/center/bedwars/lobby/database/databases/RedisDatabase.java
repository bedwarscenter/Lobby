package center.bedwars.lobby.database.databases;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.lettuce.core.api.async.RedisAsyncCommands;
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
                    .withTimeout(Duration.ofSeconds(2));

            if (!password.isEmpty()) {
                uriBuilder.withPassword(password.toCharArray());
            }

            if (ssl) {
                uriBuilder.withSsl(true);
            }

            RedisURI redisURI = uriBuilder.build();
            this.redisClient = RedisClient.create(redisURI);

            // Connection pool configuration
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

            // Test connection
            try (StatefulRedisConnection<String, String> connection = connectionPool.borrowObject()) {
                String response = connection.sync().ping();
                logger.info("Redis connection established successfully! Response: " + response);
            }

            this.running = true;

        } catch (Exception e) {
            logger.severe("Failed to connect to Redis: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Redis connection failed", e);
        }
    }

    public void disconnect() {
        this.running = false;

        // Close all subscriptions
        for (StatefulRedisPubSubConnection<byte[], byte[]> connection : subscriptions.values()) {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        }
        subscriptions.clear();

        // Close connection pool
        if (connectionPool != null && !connectionPool.isClosed()) {
            connectionPool.close();
        }

        // Shutdown client
        if (redisClient != null) {
            redisClient.shutdown();
        }

        logger.info("Redis connection closed!");
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
                logger.info("Published to channel " + channel + ", received by " + receivers + " subscribers");
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
                    logger.info("Subscribed to channel: " + new String(ch, StandardCharsets.UTF_8));
                }

                @Override
                public void unsubscribed(byte[] ch, long count) {
                    logger.info("Unsubscribed from channel: " + new String(ch, StandardCharsets.UTF_8));
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
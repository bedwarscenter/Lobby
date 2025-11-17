package center.bedwars.lobby.database.databases;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
public class RedisDatabase {

    private final Logger logger;
    private JedisPool jedisPool;
    private Thread subscriptionThread;
    private volatile boolean running = false;

    public RedisDatabase(Logger logger) {
        this.logger = logger;
    }

    public void connect() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(20);
            config.setMaxIdle(10);
            config.setMinIdle(5);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);
            config.setTestWhileIdle(true);
            config.setMinEvictableIdleTime(Duration.ofSeconds(60));
            config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
            config.setNumTestsPerEvictionRun(3);
            config.setBlockWhenExhausted(true);

            String host = SettingsConfiguration.REDIS.REDIS_HOST;
            int port = SettingsConfiguration.REDIS.REDIS_PORT;
            String password = SettingsConfiguration.REDIS.REDIS_PASSWORD;
            int database = SettingsConfiguration.REDIS.REDIS_DATABASE;
            boolean ssl = SettingsConfiguration.REDIS.REDIS_SSL;

            if (password.isEmpty()) {
                this.jedisPool = new JedisPool(config, host, port, 2000, ssl);
            } else {
                this.jedisPool = new JedisPool(config, host, port, 2000, password, database, ssl);
            }

            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                logger.info("Redis connection established successfully!");
            }

            this.running = true;

        } catch (Exception e) {
            logger.severe("Failed to connect to Redis: " + e.getMessage());
            throw new RuntimeException("Redis connection failed", e);
        }
    }

    public void disconnect() {
        this.running = false;

        if (subscriptionThread != null && subscriptionThread.isAlive()) {
            subscriptionThread.interrupt();
        }

        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection closed!");
        }
    }

    public void publish(String channel, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            logger.severe("Failed to publish message to Redis: " + e.getMessage());
        }
    }

    public void subscribe(String channel, Consumer<String> messageHandler) {
        subscriptionThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String ch, String message) {
                        if (ch.equals(channel)) {
                            messageHandler.accept(message);
                        }
                    }
                }, channel);
            } catch (Exception e) {
                if (running) {
                    logger.severe("Redis subscription error: " + e.getMessage());
                }
            }
        }, "Redis-Subscription-" + channel);

        subscriptionThread.setDaemon(true);
        subscriptionThread.start();
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            logger.severe("Failed to set key in Redis: " + e.getMessage());
        }
    }

    public void set(String key, String value, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, expireSeconds, value);
        } catch (Exception e) {
            logger.severe("Failed to set key with expiration in Redis: " + e.getMessage());
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            logger.severe("Failed to get key from Redis: " + e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.severe("Failed to delete key from Redis: " + e.getMessage());
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            logger.severe("Failed to check key existence in Redis: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        if (jedisPool == null || jedisPool.isClosed()) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.ping().equals("PONG");
        } catch (Exception e) {
            return false;
        }
    }
}
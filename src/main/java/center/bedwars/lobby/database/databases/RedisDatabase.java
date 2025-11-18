package center.bedwars.lobby.database.databases;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
public class RedisDatabase {

    private final Logger logger;
    private JedisPool jedisPool;
    private final Map<String, Thread> subscriptionThreads = new ConcurrentHashMap<>();
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
                String response = jedis.ping();
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

        for (Thread thread : subscriptionThreads.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        subscriptionThreads.clear();

        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection closed!");
        }
    }

    public void publish(String channel, String message) {
        if (!running || jedisPool == null) {
            logger.warning("Cannot publish - Redis not connected");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            long receivers = jedis.publish(channel, message);
            logger.info("Published to channel " + channel + ", received by " + receivers + " subscribers");
        } catch (Exception e) {
            logger.severe("Failed to publish message to Redis channel " + channel + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribe(String channel, Consumer<String> messageHandler) {
        if (!running) {
            logger.warning("Cannot subscribe - Redis not running");
            return;
        }

        Thread subscriptionThread = new Thread(() -> {
            while (running) {
                try (Jedis jedis = jedisPool.getResource()) {
                    logger.info("Starting subscription to channel: " + channel);

                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onSubscribe(String ch, int subscribedChannels) {
                            logger.info("Successfully subscribed to channel: " + ch);
                        }

                        @Override
                        public void onMessage(String ch, String message) {
                            if (ch.equals(channel)) {
                                try {
                                    logger.info("Received message on channel " + ch + ", size: " + message.length());
                                    messageHandler.accept(message);
                                } catch (Exception e) {
                                    logger.severe("Error processing message: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onUnsubscribe(String ch, int subscribedChannels) {
                            logger.info("Unsubscribed from channel: " + ch);
                        }
                    }, channel);
                } catch (Exception e) {
                    if (running) {
                        logger.severe("Redis subscription error: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }, "Redis-Subscription-" + channel);

        subscriptionThread.setDaemon(true);
        subscriptionThread.start();
        subscriptionThreads.put(channel, subscriptionThread);

        logger.info("Subscription thread started for channel: " + channel);
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            logger.severe("Failed to set key in Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void set(String key, String value, int expireSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, expireSeconds, value);
        } catch (Exception e) {
            logger.severe("Failed to set key with expiration in Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            logger.severe("Failed to get key from Redis: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.severe("Failed to delete key from Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            logger.severe("Failed to check key existence in Redis: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        if (jedisPool == null || jedisPool.isClosed()) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
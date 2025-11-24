package center.bedwars.lobby.database.databases;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import lombok.Getter;
import redis.clients.jedis.*;
import redis.clients.jedis.params.SetParams;

import java.nio.charset.StandardCharsets;
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

    public void publish(String channel, byte[] message) {
        if (!running || jedisPool == null) return;
        try (Jedis jedis = jedisPool.getResource()) {
            long receivers = jedis.publish(channel.getBytes(StandardCharsets.UTF_8), message);
            logger.info("Published to channel " + channel + ", received by " + receivers + " subscribers");
        } catch (Exception e) {
            logger.severe("Failed to publish message to Redis channel " + channel + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribe(String channel, Consumer<byte[]> messageHandler) {
        if (!running) return;
        Thread t = new Thread(() -> {
            while (running) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(new BinaryJedisPubSub() {
                        @Override
                        public void onSubscribe(byte[] ch, int subscribedChannels) {
                            logger.info("Subscribed to channel: " + new String(ch, StandardCharsets.UTF_8));
                        }

                        @Override
                        public void onUnsubscribe(byte[] ch, int subscribedChannels) {
                            logger.info("Unsubscribed from channel: " + new String(ch, StandardCharsets.UTF_8));
                        }

                        @Override
                        public void onMessage(byte[] ch, byte[] message) {
                            if (java.util.Arrays.equals(ch, channel.getBytes(StandardCharsets.UTF_8))) {
                                messageHandler.accept(message);
                            }
                        }
                    }, channel.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    if (running) try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
                }
            }
        }, "Redis-Subscription-" + channel);
        t.setDaemon(true);
        t.start();
        subscriptionThreads.put(channel, t);
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
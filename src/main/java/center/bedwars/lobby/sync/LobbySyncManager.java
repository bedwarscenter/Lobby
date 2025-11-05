package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.database.databases.MongoDatabaseConnection;
import center.bedwars.lobby.database.databases.RedisDatabase;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.sync.handlers.*;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter
public class LobbySyncManager extends Manager {

    private static final String REDIS_SYNC_CHANNEL = "bedwars:lobby:sync";
    private static final String MONGO_COLLECTION = "lobby_sync_events";

    private final Lobby lobby = Lobby.getINSTANCE();
    private final Logger logger = lobby.getLogger();
    private final String lobbyId = SettingsConfiguration.LOBBY_ID;

    private RedisDatabase redis;
    private MongoDatabaseConnection mongo;
    private MongoCollection<Document> syncCollection;

    private final Map<SyncEventType, ISyncHandler> handlers = new HashMap<>();
    private final ConcurrentLinkedQueue<SyncEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService batchExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean processing = true;
    private long lastEventTime = 0;
    private static final long EVENT_COOLDOWN_MS = 50;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int BATCH_SIZE = 20;

    @Override
    protected void onLoad() {
        DatabaseManager dbManager = Lobby.getManagerStorage().getManager(DatabaseManager.class);

        this.redis = dbManager.getRedis();
        this.mongo = dbManager.getMongo();

        if (!redis.isConnected() || !mongo.isConnected()) {
            throw new IllegalStateException("Redis or MongoDB is not connected!");
        }

        this.syncCollection = mongo.getCollection(MONGO_COLLECTION);

        registerHandlers();
        setupRedisSubscription();
        setupBatchProcessing();
    }

    @Override
    protected void onUnload() {
        this.processing = false;
        handlers.clear();
        batchExecutor.shutdown();
        eventQueue.clear();
    }

    private void registerHandlers() {
        handlers.put(SyncEventType.BLOCK_PLACE, new BlockSyncHandler());
        handlers.put(SyncEventType.BLOCK_BREAK, new BlockSyncHandler());
        handlers.put(SyncEventType.NPC_CREATE, new NPCSyncHandler());
        handlers.put(SyncEventType.NPC_DELETE, new NPCSyncHandler());
        handlers.put(SyncEventType.NPC_UPDATE, new NPCSyncHandler());
        handlers.put(SyncEventType.HOLOGRAM_CREATE, new HologramSyncHandler());
        handlers.put(SyncEventType.HOLOGRAM_DELETE, new HologramSyncHandler());
        handlers.put(SyncEventType.HOLOGRAM_UPDATE, new HologramSyncHandler());
        handlers.put(SyncEventType.CHUNK_SNAPSHOT, new ChunkSnapshotSyncHandler());
        handlers.put(SyncEventType.CONFIG_PUSH, new ConfigSyncHandler());
    }

    private void setupRedisSubscription() {
        redis.subscribe(REDIS_SYNC_CHANNEL, message -> {
            if (!processing) return;

            if (eventQueue.size() >= MAX_QUEUE_SIZE) {
                eventQueue.poll();
            }

            try {
                SyncEvent event = SyncDataSerializer.deserialize(message);

                if (event.isFromSameLobby(lobbyId)) {
                    return;
                }

                eventQueue.offer(event);

            } catch (Exception e) {
                logger.severe("Failed to process sync event: " + e.getMessage());
            }
        });
    }

    private void setupBatchProcessing() {
        batchExecutor.scheduleAtFixedRate(() -> {
            if (!processing || eventQueue.isEmpty()) return;

            int processed = 0;
            SyncEvent event;
            while ((event = eventQueue.poll()) != null && processed < BATCH_SIZE) {
                handleSyncEventImmediate(event);
                processed++;

                if (processed % 5 == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void broadcastEvent(SyncEventType type, JsonObject data) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEventTime < EVENT_COOLDOWN_MS) {
            return;
        }
        lastEventTime = currentTime;

        if (data.toString().length() > 10000) {
            logger.warning("Sync event data too large: " + type);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                SyncEvent event = new SyncEvent(lobbyId, type, data);
                String serialized = SyncDataSerializer.serialize(event);

                redis.publish(REDIS_SYNC_CHANNEL, serialized);

                Document doc = Document.parse(serialized);
                syncCollection.insertOne(doc);

            } catch (Exception e) {
                logger.severe("Failed to broadcast sync event: " + e.getMessage());
            }
        });
    }

    private void handleSyncEventImmediate(SyncEvent event) {
        ISyncHandler handler = handlers.get(event.getType());

        if (handler == null) {
            return;
        }

        try {
            handler.handle(event);
        } catch (Exception e) {
            logger.severe("Error handling sync event: " + e.getMessage());
        }
    }

    public void performFullSync() {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting full lobby synchronization...");

                JsonObject data = new JsonObject();
                data.addProperty("requestingLobby", lobbyId);

                broadcastEvent(SyncEventType.FULL_SYNC, data);

            } catch (Exception e) {
                logger.severe("Failed to perform full sync: " + e.getMessage());
            }
        });
    }
}
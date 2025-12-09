package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.IDatabaseService;
import center.bedwars.lobby.service.AbstractService;
import center.bedwars.lobby.sync.handlers.*;
import center.bedwars.lobby.sync.serialization.Serializer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.*;

@Singleton
@Getter
public class LobbySyncService extends AbstractService implements ILobbySyncService {

    private static final String REDIS_CHANNEL = "bwl:ls";
    private static final String MONGO_COLLECTION = "lobby_sync_events";
    private static final int BATCH_SIZE = 15;
    private static final int BATCH_INTERVAL_MS = 100;
    private static final int MAX_QUEUE_SIZE = 500;
    private static final int MAX_DATA_SIZE = 40000;

    private final Lobby plugin;
    private final IDatabaseService databaseService;
    private final com.google.inject.Injector injector;
    private final String lobbyId = SettingsConfiguration.LOBBY_ID;
    private center.bedwars.lobby.database.IRedisService redis;
    private com.mongodb.client.MongoCollection<Document> syncCollection;
    private final Map<SyncEventType, ISyncHandler> handlers = new ConcurrentHashMap<>();
    private final BlockingQueue<SyncEvent> eventQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile boolean processing = true;

    @Inject
    public LobbySyncService(Lobby plugin, IDatabaseService databaseService, com.google.inject.Injector injector) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.injector = injector;
    }

    @Override
    protected void onEnable() {
        this.redis = databaseService.getRedis();
        this.syncCollection = databaseService.getMongo().getCollection(MONGO_COLLECTION);
        registerHandlers();
        setupSubscription();
        startBatchProcessor();
    }

    @Override
    protected void onDisable() {
        processing = false;
        executor.shutdownNow();
        handlers.clear();
        eventQueue.clear();
    }

    private void registerHandlers() {
        ISyncHandler blockHandler = injector.getInstance(BlockSyncHandler.class);
        handlers.put(SyncEventType.BLOCK_PLACE, blockHandler);
        handlers.put(SyncEventType.BLOCK_BREAK, blockHandler);

        ISyncHandler npcHandler = injector.getInstance(NPCSyncHandler.class);
        handlers.put(SyncEventType.NPC_CREATE, npcHandler);
        handlers.put(SyncEventType.NPC_DELETE, npcHandler);
        handlers.put(SyncEventType.NPC_UPDATE, npcHandler);

        ISyncHandler hologramHandler = injector.getInstance(HologramSyncHandler.class);
        handlers.put(SyncEventType.HOLOGRAM_CREATE, hologramHandler);
        handlers.put(SyncEventType.HOLOGRAM_DELETE, hologramHandler);
        handlers.put(SyncEventType.HOLOGRAM_UPDATE, hologramHandler);

        handlers.put(SyncEventType.CHUNK_SNAPSHOT, injector.getInstance(ChunkSnapshotSyncHandler.class));
        handlers.put(SyncEventType.CONFIG_PUSH, injector.getInstance(ConfigSyncHandler.class));
        handlers.put(SyncEventType.WORLD_SYNC, injector.getInstance(WorldSyncHandler.class));
        handlers.put(SyncEventType.PARKOUR_SYNC, injector.getInstance(ParkourSyncHandler.class));
        handlers.put(SyncEventType.FULL_SYNC, injector.getInstance(FullSyncHandler.class));
    }

    private void setupSubscription() {
        redis.subscribe(REDIS_CHANNEL, this::processEvent);
    }

    private void startBatchProcessor() {
        executor.scheduleAtFixedRate(() -> {
            if (!processing || eventQueue.isEmpty())
                return;
            int processed = 0;
            SyncEvent event;
            while ((event = eventQueue.poll()) != null && processed < BATCH_SIZE) {
                handleEvent(event);
                processed++;
            }
        }, 0, BATCH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void processEvent(byte[] raw) {
        try {
            SyncEvent event = Serializer.deserialize(raw, SyncEvent.class);
            if (event.isFromSameLobby(lobbyId))
                return;
            if (!eventQueue.offer(event)) {
                eventQueue.poll();
                eventQueue.offer(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void broadcastEvent(SyncEventType type, byte[] data) {
        if (data.length > MAX_DATA_SIZE)
            return;
        CompletableFuture.runAsync(() -> {
            try {
                SyncEvent event = new SyncEvent(lobbyId, type, data);
                byte[] serialized = Serializer.serialize(event);
                redis.publish(REDIS_CHANNEL, serialized);

                executor.schedule(() -> {
                    try {
                        Document doc = new Document()
                                .append("lobby", lobbyId)
                                .append("type", type.name())
                                .append("time", System.currentTimeMillis())
                                .append("data", java.util.Base64.getEncoder().encodeToString(serialized));
                        syncCollection.insertOne(doc);
                    } catch (Exception ignored) {
                    }
                }, 500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleEvent(SyncEvent event) {
        ISyncHandler handler = handlers.get(event.getType());
        if (handler != null) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void performFullSync() {
        byte[] data = new byte[] { 1 };
        broadcastEvent(SyncEventType.FULL_SYNC, data);
    }

    @Override
    public void syncLobby() {
        performFullSync();
    }

    @Override
    public void requestSync() {
        performFullSync();
    }
}

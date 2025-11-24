package center.bedwars.lobby.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.database.DatabaseManager;
import center.bedwars.lobby.manager.Manager;
import center.bedwars.lobby.sync.handlers.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@Getter
public class LobbySyncManager extends Manager {

    private static final String REDIS_CHANNEL = "bwl:ls";
    private static final String MONGO_COLLECTION = "lobby_sync_events";
    private static final int BATCH_SIZE = 15;
    private static final int BATCH_INTERVAL_MS = 100;
    private static final int MAX_QUEUE_SIZE = 500;
    private static final int MAX_DATA_SIZE = 40000;

    private final String lobbyId = SettingsConfiguration.LOBBY_ID;
    private center.bedwars.lobby.database.databases.RedisDatabase redis;
    private com.mongodb.client.MongoCollection<Document> syncCollection;
    private final Map<SyncEventType, ISyncHandler> handlers = new ConcurrentHashMap<>();
    private final BlockingQueue<SyncEvent> eventQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile boolean processing = true;

    @Override
    protected void onLoad() {
        DatabaseManager dbManager = Lobby.getManagerStorage().getManager(DatabaseManager.class);
        this.redis = dbManager.getRedis();
        this.syncCollection = dbManager.getMongo().getCollection(MONGO_COLLECTION);
        registerHandlers();
        setupSubscription();
        startBatchProcessor();
    }

    @Override
    protected void onUnload() {
        processing = false;
        executor.shutdownNow();
        handlers.clear();
        eventQueue.clear();
    }

    private void registerHandlers() {
        ISyncHandler blockHandler = new BlockSyncHandler();
        handlers.put(SyncEventType.BLOCK_PLACE, blockHandler);
        handlers.put(SyncEventType.BLOCK_BREAK, blockHandler);
        ISyncHandler npcHandler = new NPCSyncHandler();
        handlers.put(SyncEventType.NPC_CREATE, npcHandler);
        handlers.put(SyncEventType.NPC_DELETE, npcHandler);
        handlers.put(SyncEventType.NPC_UPDATE, npcHandler);
        ISyncHandler hologramHandler = new HologramSyncHandler();
        handlers.put(SyncEventType.HOLOGRAM_CREATE, hologramHandler);
        handlers.put(SyncEventType.HOLOGRAM_DELETE, hologramHandler);
        handlers.put(SyncEventType.HOLOGRAM_UPDATE, hologramHandler);
        handlers.put(SyncEventType.CHUNK_SNAPSHOT, new ChunkSnapshotSyncHandler());
        handlers.put(SyncEventType.CONFIG_PUSH, new ConfigSyncHandler());
        handlers.put(SyncEventType.WORLD_SYNC, new WorldSyncHandler());
        handlers.put(SyncEventType.PARKOUR_SYNC, new ParkourSyncHandler());
        handlers.put(SyncEventType.FULL_SYNC, new FullSyncHandler());
    }

    private void setupSubscription() {
        redis.subscribe(REDIS_CHANNEL, this::processCompressed);
    }

    private void startBatchProcessor() {
        executor.scheduleAtFixedRate(() -> {
            if (!processing || eventQueue.isEmpty()) return;
            int processed = 0;
            SyncEvent event;
            while ((event = eventQueue.poll()) != null && processed < BATCH_SIZE) {
                handleEvent(event);
                processed++;
            }
        }, 0, BATCH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void processCompressed(byte[] raw) {
        try {
            byte[] compressed = raw;
            ByteBuf packet = Unpooled.wrappedBuffer(decompress(compressed));
            SyncEvent event = SyncEvent.deserialize(packet);
            if (event.isFromSameLobby(lobbyId)) return;
            if (!eventQueue.offer(event)) {
                eventQueue.poll();
                eventQueue.offer(event);
            }
        } catch (Exception ignored) {}
    }

    public void broadcastEvent(SyncEventType type, ByteBuf data) {
        if (data.readableBytes() > MAX_DATA_SIZE) return;
        CompletableFuture.runAsync(() -> {
            try {
                SyncEvent event = new SyncEvent(lobbyId, type, data);
                ByteBuf packet = event.serialize();
                byte[] compressed = compress(packet.array());
                redis.publish(REDIS_CHANNEL, compressed);
                executor.schedule(() -> {
                    try {
                        Document doc = new Document()
                                .append("lobby", lobbyId)
                                .append("type", type.name())
                                .append("time", System.currentTimeMillis())
                                .append("data", java.util.Base64.getEncoder().encodeToString(compressed));
                        syncCollection.insertOne(doc);
                    } catch (Exception ignored) {}
                }, 500, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}
        });
    }

    private void handleEvent(SyncEvent event) {
        ISyncHandler handler = handlers.get(event.getType());
        if (handler != null) {
            try {
                handler.handle(event);
            } catch (Exception ignored) {}
        }
    }

    public void performFullSync() {
        ByteBuf data = Unpooled.buffer();
        data.writeBoolean(true);
        broadcastEvent(SyncEventType.FULL_SYNC, data);
    }

    private byte[] compress(byte[] input) throws Exception {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(input);
        deflater.finish();
        byte[] output = new byte[input.length];
        int size = deflater.deflate(output);
        deflater.end();
        byte[] result = new byte[size];
        System.arraycopy(output, 0, result, 0, size);
        return result;
    }

    private byte[] decompress(byte[] compressed) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        byte[] result = new byte[compressed.length * 4];
        int size = inflater.inflate(result);
        inflater.end();
        byte[] output = new byte[size];
        System.arraycopy(result, 0, output, 0, size);
        return output;
    }
}
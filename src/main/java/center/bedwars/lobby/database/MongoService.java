package center.bedwars.lobby.database;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import center.bedwars.lobby.service.AbstractService;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
public class MongoService extends AbstractService implements IMongoService {

    private final Logger logger;
    private MongoClient mongoClient;
    private MongoDatabase database;

    @Inject
    public MongoService(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void onEnable() {
        try {
            String uri = SettingsConfiguration.MONGO.MONGO_URI;
            String dbName = SettingsConfiguration.MONGO.MONGO_DATABASE;
            int timeout = SettingsConfiguration.MONGO.MONGO_CONNECTION_TIMEOUT;

            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSocketSettings(builder -> builder.connectTimeout(timeout, TimeUnit.MILLISECONDS)
                            .readTimeout(timeout, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(timeout, TimeUnit.MILLISECONDS));

            this.mongoClient = MongoClients.create(settingsBuilder.build());
            this.database = mongoClient.getDatabase(dbName);

            database.runCommand(new Document("ping", 1));
            logger.info("Connected to MongoDB");

        } catch (Exception e) {
            logger.severe("Failed to connect to MongoDB: " + e.getMessage());
            throw new RuntimeException("MongoDB connection failed", e);
        }
    }

    @Override
    protected void onDisable() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public MongoCollection<Document> getCollection(String collectionName) {
        if (database == null) {
            throw new IllegalStateException("MongoDB is not connected!");
        }
        return database.getCollection(collectionName);
    }

    @Override
    public CompletableFuture<Document> findOneAsync(String collection, Bson filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCollection(collection).find(filter).first();
            } catch (Exception e) {
                logger.severe("Failed to find document in MongoDB: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<Document>> findAsync(String collection, Bson filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCollection(collection).find(filter).into(new ArrayList<>());
            } catch (Exception e) {
                logger.severe("Failed to find documents in MongoDB: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertAsync(String collection, Document document) {
        return CompletableFuture.runAsync(() -> {
            try {
                getCollection(collection).insertOne(document);
            } catch (Exception e) {
                logger.severe("Failed to insert document in MongoDB: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateAsync(String collection, Bson filter, Bson update) {
        return CompletableFuture.runAsync(() -> {
            try {
                getCollection(collection).updateOne(filter, update);
            } catch (Exception e) {
                logger.severe("Failed to update document in MongoDB: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteAsync(String collection, Bson filter) {
        return CompletableFuture.runAsync(() -> {
            try {
                getCollection(collection).deleteOne(filter);
            } catch (Exception e) {
                logger.severe("Failed to delete document in MongoDB: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean isConnected() {
        if (mongoClient == null || database == null) {
            return false;
        }

        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

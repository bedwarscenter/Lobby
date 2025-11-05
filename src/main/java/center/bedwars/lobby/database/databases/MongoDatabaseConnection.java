package center.bedwars.lobby.database.databases;

import center.bedwars.lobby.configuration.configurations.SettingsConfiguration;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter
public class MongoDatabaseConnection {

    private final Logger logger;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDatabaseConnection(Logger logger) {
        this.logger = logger;
    }

    public void connect() {
        try {
            String uri = SettingsConfiguration.MONGO.MONGO_URI;
            String dbName = SettingsConfiguration.MONGO.MONGO_DATABASE;
            int timeout = SettingsConfiguration.MONGO.MONGO_CONNECTION_TIMEOUT;

            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToSocketSettings(builder ->
                            builder.connectTimeout(timeout, TimeUnit.MILLISECONDS)
                                    .readTimeout(timeout, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder ->
                            builder.serverSelectionTimeout(timeout, TimeUnit.MILLISECONDS));

            this.mongoClient = MongoClients.create(settingsBuilder.build());
            this.database = mongoClient.getDatabase(dbName);

            // Test connection
            database.runCommand(new Document("ping", 1));
            logger.info("MongoDB connection established successfully!");

        } catch (Exception e) {
            logger.severe("Failed to connect to MongoDB: " + e.getMessage());
            throw new RuntimeException("MongoDB connection failed", e);
        }
    }

    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed!");
        }
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        if (database == null) {
            throw new IllegalStateException("MongoDB is not connected!");
        }
        return database.getCollection(collectionName);
    }

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
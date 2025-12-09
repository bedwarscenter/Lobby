package center.bedwars.lobby.database;

import center.bedwars.lobby.service.IService;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IMongoService extends IService {
    MongoCollection<Document> getCollection(String collectionName);

    CompletableFuture<Document> findOneAsync(String collection, Bson filter);

    CompletableFuture<List<Document>> findAsync(String collection, Bson filter);

    CompletableFuture<Void> insertAsync(String collection, Document document);

    CompletableFuture<Void> updateAsync(String collection, Bson filter, Bson update);

    CompletableFuture<Void> deleteAsync(String collection, Bson filter);

    boolean isConnected();
}

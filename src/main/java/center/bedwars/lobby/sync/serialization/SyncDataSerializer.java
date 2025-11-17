package center.bedwars.lobby.sync.serialization;

import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;
import org.bukkit.Material;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.*;

public class SyncDataSerializer {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final String GZIP_PREFIX = "GZIP:";

    public static String serialize(SyncEvent event) {
        JsonObject json = new JsonObject();
        json.addProperty("sourceLobby", event.getSourceLobby());
        json.addProperty("type", event.getType().getIdentifier());
        json.addProperty("timestamp", event.getTimestamp());
        json.add("data", event.getData());

        String jsonString = GSON.toJson(json);

        if (jsonString.length() > 1000) {
            try {
                return GZIP_PREFIX + compressData(jsonString);
            } catch (IOException e) {
                return jsonString;
            }
        }

        return jsonString;
    }

    public static SyncEvent deserialize(String json) {
        try {
            String dataToProcess;

            if (json.startsWith(GZIP_PREFIX)) {
                dataToProcess = decompressData(json.substring(GZIP_PREFIX.length()));
            } else if (json.startsWith("{")) {
                dataToProcess = json;
            } else {
                dataToProcess = decompressData(json);
            }

            JsonObject obj = JSON_PARSER.parse(dataToProcess).getAsJsonObject();
            String sourceLobby = obj.get("sourceLobby").getAsString();
            SyncEventType type = SyncEventType.fromIdentifier(obj.get("type").getAsString());
            long timestamp = obj.get("timestamp").getAsLong();
            JsonObject data = obj.getAsJsonObject("data");
            return new SyncEvent(sourceLobby, type, timestamp, data);

        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize sync event: " + e.getMessage(), e);
        }
    }

    private static String compressData(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    private static String decompressData(String compressedData) throws IOException {
        byte[] data = Base64.getDecoder().decode(compressedData);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(bis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }

    public static JsonObject serializeLocation(Location loc) {
        JsonObject json = new JsonObject();
        json.addProperty("world", loc.getWorld().getName());
        json.addProperty("x", loc.getX());
        json.addProperty("y", loc.getY());
        json.addProperty("z", loc.getZ());
        json.addProperty("yaw", loc.getYaw());
        json.addProperty("pitch", loc.getPitch());
        return json;
    }

    public static Location deserializeLocation(JsonObject json, org.bukkit.Server server) {
        String worldName = json.get("world").getAsString();
        double x = json.get("x").getAsDouble();
        double y = json.get("y").getAsDouble();
        double z = json.get("z").getAsDouble();
        float yaw = json.get("yaw").getAsFloat();
        float pitch = json.get("pitch").getAsFloat();

        return new Location(server.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public static JsonObject serializeBlockData(Location loc, Material material, byte data) {
        JsonObject json = new JsonObject();
        json.add("location", serializeLocation(loc));
        json.addProperty("material", material.name());
        json.addProperty("data", data);
        return json;
    }

    public static JsonObject serializeChunkSnapshot(int chunkX, int chunkZ, byte[] snapshotData) {
        JsonObject json = new JsonObject();
        json.addProperty("chunkX", chunkX);
        json.addProperty("chunkZ", chunkZ);
        json.addProperty("data", Base64.getEncoder().encodeToString(snapshotData));
        return json;
    }

    public static byte[] deserializeChunkSnapshotData(JsonObject json) {
        String encoded = json.get("data").getAsString();
        return Base64.getDecoder().decode(encoded);
    }

    public static JsonObject serializeNPCData(String npcId, Location loc, String skinTexture, String skinSignature, String displayName) {
        JsonObject json = new JsonObject();
        json.addProperty("npcId", npcId);
        json.add("location", serializeLocation(loc));
        if (skinTexture != null && !skinTexture.isEmpty()) {
            json.addProperty("skinTexture", skinTexture);
        }
        if (skinSignature != null && !skinSignature.isEmpty()) {
            json.addProperty("skinSignature", skinSignature);
        }
        json.addProperty("displayName", displayName);
        return json;
    }

    public static JsonObject serializeHologramData(String hologramId, Location loc, String[] lines) {
        JsonObject json = new JsonObject();
        json.addProperty("hologramId", hologramId);
        json.add("location", serializeLocation(loc));
        if (lines != null && lines.length > 0) {
            json.addProperty("lines", GSON.toJson(lines));
        }
        return json;
    }

    public static String[] deserializeHologramLines(JsonObject json) {
        String linesJson = json.get("lines").getAsString();
        return GSON.fromJson(linesJson, String[].class);
    }
}
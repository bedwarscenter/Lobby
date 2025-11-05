package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.dependency.dependencies.CitizensDependency;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class NPCSyncHandler implements ISyncHandler {

    private final CitizensDependency citizens;
    private final NPCRegistry registry;

    public NPCSyncHandler() {
        DependencyManager depManager = Lobby.getManagerStorage().getManager(DependencyManager.class);
        this.citizens = depManager.getCitizens();

        if (!citizens.isApiAvailable()) {
            throw new IllegalStateException("Citizens API is not available!");
        }

        this.registry = citizens.getNpcRegistry();
    }

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        String npcId = data.get("npcId").getAsString();

        switch (event.getType()) {
            case NPC_CREATE:
                handleCreate(data, npcId);
                break;
            case NPC_DELETE:
                handleDelete(npcId);
                break;
            case NPC_UPDATE:
                handleUpdate(data, npcId);
                break;
        }
    }

    private void handleCreate(JsonObject data, String npcId) {
        Location loc = SyncDataSerializer.deserializeLocation(
                data.getAsJsonObject("location"),
                Lobby.getINSTANCE().getServer()
        );

        String displayName = data.get("displayName").getAsString();

        NPC npc = registry.createNPC(EntityType.PLAYER, displayName, loc);

        if (data.has("skinTexture") && data.has("skinSignature")) {
            String texture = data.get("skinTexture").getAsString();
            String signature = data.get("skinSignature").getAsString();

            if (!texture.isEmpty() && !signature.isEmpty()) {
                applySkin(npc, texture, signature);
            }
        }
    }

    private void handleDelete(String npcId) {
        NPC npc = findNPCById(npcId);
        if (npc != null) {
            npc.destroy();
        }
    }

    private void handleUpdate(JsonObject data, String npcId) {
        NPC npc = findNPCById(npcId);
        if (npc == null) return;

        if (data.has("location")) {
            Location loc = SyncDataSerializer.deserializeLocation(
                    data.getAsJsonObject("location"),
                    Lobby.getINSTANCE().getServer()
            );

            if (npc.isSpawned()) {
                npc.teleport(loc, null);
            } else {
                npc.spawn(loc);
            }
        }

        if (data.has("displayName")) {
            npc.setName(data.get("displayName").getAsString());
        }

        if (data.has("skinTexture") && data.has("skinSignature")) {
            String texture = data.get("skinTexture").getAsString();
            String signature = data.get("skinSignature").getAsString();
            applySkin(npc, texture, signature);
        }
    }

    private void applySkin(NPC npc, String texture, String signature) {
        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinPersistent(
                npc.getUniqueId().toString(),
                signature,
                texture
        );
    }

    private NPC findNPCById(String npcId) {
        for (NPC npc : registry) {
            if (npc.getName().equals(npcId)) {
                return npc;
            }
        }
        return null;
    }
}
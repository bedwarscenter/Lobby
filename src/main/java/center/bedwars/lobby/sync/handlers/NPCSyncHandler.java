package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.dependency.DependencyManager;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import com.google.gson.JsonObject;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class NPCSyncHandler implements ISyncHandler {

    private final NPCRegistry registry;

    public NPCSyncHandler() {
        this.registry = Lobby.getManagerStorage()
                .getManager(DependencyManager.class)
                .getCitizens()
                .getNpcRegistry();
    }

    @Override
    public void handle(SyncEvent event) {
        JsonObject data = event.getData();
        String npcId = data.get("id").getAsString();

        switch (event.getType()) {
            case NPC_CREATE -> handleCreate(data, npcId);
            case NPC_DELETE -> handleDelete(npcId);
            case NPC_UPDATE -> handleUpdate(data, npcId);
        }
    }

    private void handleCreate(JsonObject data, String npcId) {
        Location loc = SyncDataSerializer.deserializeLocation(
                data.getAsJsonObject("loc"), Lobby.getINSTANCE().getServer());

        NPC npc = registry.createNPC(EntityType.PLAYER, data.get("name").getAsString(), loc);

        if (data.has("tex") && data.has("sig")) {
            applySkin(npc, data.get("tex").getAsString(), data.get("sig").getAsString());
        }
    }

    private void handleDelete(String npcId) {
        NPC npc = findNPC(npcId);
        if (npc != null) npc.destroy();
    }

    private void handleUpdate(JsonObject data, String npcId) {
        NPC npc = findNPC(npcId);
        if (npc == null) return;

        if (data.has("loc")) {
            Location loc = SyncDataSerializer.deserializeLocation(
                    data.getAsJsonObject("loc"), Lobby.getINSTANCE().getServer());
            if (npc.isSpawned()) {
                npc.teleport(loc, null);
            } else {
                npc.spawn(loc);
            }
        }

        if (data.has("name")) {
            npc.setName(data.get("name").getAsString());
        }

        if (data.has("tex") && data.has("sig")) {
            applySkin(npc, data.get("tex").getAsString(), data.get("sig").getAsString());
        }
    }

    private void applySkin(NPC npc, String texture, String signature) {
        SkinTrait skin = npc.getOrAddTrait(SkinTrait.class);
        skin.setSkinPersistent(npc.getUniqueId().toString(), signature, texture);
    }

    private NPC findNPC(String npcId) {
        for (NPC npc : registry) {
            if (npc.getName().equals(npcId)) return npc;
        }
        return null;
    }
}
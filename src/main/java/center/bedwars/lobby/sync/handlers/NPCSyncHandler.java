package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.KryoSerializer;
import center.bedwars.lobby.sync.serialization.KryoSerializer.NPCData;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class NPCSyncHandler implements ISyncHandler {

    private final NPCRegistry registry;

    public NPCSyncHandler() {
        this.registry = Lobby.getManagerStorage()
                .getManager(center.bedwars.lobby.dependency.DependencyManager.class)
                .getCitizens()
                .getNpcRegistry();
    }

    @Override
    public void handle(SyncEvent event) {
        try {
            NPCData npcData = KryoSerializer.deserialize(event.getData(), NPCData.class);

            Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
                try {
                    if (event.getType() == SyncEventType.NPC_CREATE) {
                        handleCreate(npcData);
                    } else if (event.getType() == SyncEventType.NPC_DELETE) {
                        handleDelete(npcData.npcId);
                    } else if (event.getType() == SyncEventType.NPC_UPDATE) {
                        handleUpdate(npcData);
                    }
                } catch (Exception e) {
                    Lobby.getINSTANCE().getLogger().warning("Failed to sync NPC: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCreate(NPCData data) {
        Location loc = data.location.toLocation(Lobby.getINSTANCE().getServer());
        if (loc == null) return;

        NPC existingNpc = findNPC(data.npcId);
        if (existingNpc != null) {
            existingNpc.destroy();
        }

        NPC npc = registry.createNPC(EntityType.PLAYER, data.name);
        npc.spawn(loc);

        if (!data.texture.isEmpty() && !data.signature.isEmpty()) {
            applySkin(npc, data.texture, data.signature);
        }
    }

    private void handleDelete(String npcId) {
        NPC npc = findNPC(npcId);
        if (npc != null) {
            npc.destroy();
        }
    }

    private void handleUpdate(NPCData data) {
        NPC npc = findNPC(data.npcId);
        if (npc == null) return;

        Location loc = data.location.toLocation(Lobby.getINSTANCE().getServer());
        if (loc != null) {
            if (npc.isSpawned()) {
                npc.teleport(loc, null);
            } else {
                npc.spawn(loc);
            }
        }

        npc.setName(data.name);

        if (!data.texture.isEmpty() && !data.signature.isEmpty()) {
            applySkin(npc, data.texture, data.signature);
        }
    }

    private void applySkin(NPC npc, String texture, String signature) {
        SkinTrait skin = npc.getOrAddTrait(SkinTrait.class);
        skin.setSkinPersistent(npc.getUniqueId().toString(), signature, texture);
    }

    private NPC findNPC(String npcId) {
        try {
            int id = Integer.parseInt(npcId);
            return registry.getById(id);
        } catch (NumberFormatException e) {
            for (NPC npc : registry) {
                if (String.valueOf(npc.getId()).equals(npcId)) {
                    return npc;
                }
            }
        }
        return null;
    }
}
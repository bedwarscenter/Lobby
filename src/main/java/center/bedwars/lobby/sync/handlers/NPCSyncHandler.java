package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import io.netty.buffer.ByteBuf;
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
        ByteBuf data = event.getData();
        String npcId = SyncDataSerializer.readUTF(data);
        Bukkit.getScheduler().runTask(Lobby.getINSTANCE(), () -> {
            try {
                if (event.getType() == SyncEventType.NPC_CREATE) {
                    handleCreate(data, npcId);
                } else if (event.getType() == SyncEventType.NPC_DELETE) {
                    handleDelete(npcId);
                } else if (event.getType() == SyncEventType.NPC_UPDATE) {
                    handleUpdate(data, npcId);
                }
            } catch (Exception e) {
                Lobby.getINSTANCE().getLogger().warning("Failed to sync NPC: " + e.getMessage());
            }
        });
    }

    private void handleCreate(ByteBuf data, String npcId) {
        Location loc = SyncDataSerializer.deserializeLocation(data, Lobby.getINSTANCE().getServer());
        if (loc == null) return;
        String name = SyncDataSerializer.readUTF(data);
        NPC existingNpc = findNPC(npcId);
        if (existingNpc != null) {
            existingNpc.destroy();
        }
        NPC npc = registry.createNPC(EntityType.PLAYER, name);
        npc.spawn(loc);
        String texture = SyncDataSerializer.readUTF(data);
        String signature = SyncDataSerializer.readUTF(data);
        if (!texture.isEmpty() && !signature.isEmpty()) {
            applySkin(npc, texture, signature);
        }
    }

    private void handleDelete(String npcId) {
        NPC npc = findNPC(npcId);
        if (npc != null) {
            npc.destroy();
        }
    }

    private void handleUpdate(ByteBuf data, String npcId) {
        NPC npc = findNPC(npcId);
        if (npc == null) return;
        if (data.readableBytes() > 0) {
            Location loc = SyncDataSerializer.deserializeLocation(data, Lobby.getINSTANCE().getServer());
            if (loc != null) {
                if (npc.isSpawned()) {
                    npc.teleport(loc, null);
                } else {
                    npc.spawn(loc);
                }
            }
        }
        String name = SyncDataSerializer.readUTF(data);
        npc.setName(name);
        String texture = SyncDataSerializer.readUTF(data);
        String signature = SyncDataSerializer.readUTF(data);
        if (!texture.isEmpty() && !signature.isEmpty()) {
            applySkin(npc, texture, signature);
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
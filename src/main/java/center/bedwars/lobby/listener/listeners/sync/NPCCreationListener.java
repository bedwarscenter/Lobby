package center.bedwars.lobby.listener.listeners.sync;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.sync.LobbySyncManager;
import center.bedwars.lobby.sync.SyncEventType;
import center.bedwars.lobby.sync.serialization.SyncDataSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.citizensnpcs.api.event.NPCCreateEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class NPCCreationListener implements Listener {

    private final LobbySyncManager syncManager;

    public NPCCreationListener() {
        this.syncManager = Lobby.getManagerStorage().getManager(LobbySyncManager.class);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNPCCreate(NPCCreateEvent event) {
        NPC npc = event.getNPC();
        if (npc.getStoredLocation() == null) return;
        String texture = "";
        String signature = "";
        if (npc.hasTrait(SkinTrait.class)) {
            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            texture = skinTrait.getTexture();
            signature = skinTrait.getSignature();
        }
        ByteBuf data = SyncDataSerializer.serializeNPCData(String.valueOf(npc.getId()), npc.getName(), npc.getStoredLocation(), texture, signature);
        syncManager.broadcastEvent(SyncEventType.NPC_CREATE, data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNPCRemove(NPCRemoveEvent event) {
        NPC npc = event.getNPC();
        ByteBuf data = Unpooled.buffer();
        SyncDataSerializer.writeUTF(data, String.valueOf(npc.getId()));
        syncManager.broadcastEvent(SyncEventType.NPC_DELETE, data);
    }
}
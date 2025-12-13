package center.bedwars.lobby.sync.handlers;

import center.bedwars.lobby.Lobby;
import center.bedwars.lobby.constant.LogMessages;
import center.bedwars.lobby.constant.NPCConstants;
import center.bedwars.lobby.dependency.IDependencyService;
import center.bedwars.lobby.dependency.dependencies.ZNPCsPlusDependency;
import center.bedwars.lobby.sync.SyncEvent;
import center.bedwars.lobby.sync.serialization.Serializer;
import com.google.inject.Inject;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.api.npc.NpcEntry;
import lol.pyr.znpcsplus.api.npc.NpcRegistry;
import lol.pyr.znpcsplus.api.npc.NpcType;
import lol.pyr.znpcsplus.api.skin.SkinDescriptor;
import lol.pyr.znpcsplus.util.NpcLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class NPCSyncHandler implements ISyncHandler {

    private final Lobby plugin;
    private final ZNPCsPlusDependency znpcsPlus;

    @Inject
    public NPCSyncHandler(Lobby plugin, IDependencyService dependencyService) {
        this.plugin = plugin;
        this.znpcsPlus = dependencyService.getZNPCsPlus();
    }

    @Override
    public void handle(SyncEvent event) {
        if (!znpcsPlus.isApiAvailable()) {
            plugin.getLogger().warning(LogMessages.ZNPCS_NOT_AVAILABLE);
            return;
        }

        Serializer.NPCData npcData = deserializeNpcData(event);
        if (npcData == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> processNpcEvent(event, npcData));
    }

    private Serializer.NPCData deserializeNpcData(SyncEvent event) {
        try {
            return Serializer.deserialize(event.getData(), Serializer.NPCData.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void processNpcEvent(SyncEvent event, Serializer.NPCData npcData) {
        try {
            switch (event.getType()) {
                case NPC_CREATE:
                    handleCreate(npcData);
                    break;
                case NPC_DELETE:
                    handleDelete(npcData.npcId);
                    break;
                case NPC_UPDATE:
                    handleUpdate(npcData);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning(String.format(LogMessages.NPC_SYNC_FAILED, e.getMessage()));
        }
    }

    private void handleCreate(Serializer.NPCData data) {
        NpcRegistry registry = znpcsPlus.getNpcRegistry();
        if (registry == null) {
            return;
        }

        Location loc = data.location.toLocation(Bukkit.getServer());
        if (loc == null || loc.getWorld() == null) {
            return;
        }

        String npcId = NPCConstants.NPC_ID_PREFIX + data.npcId;
        deleteExistingNpc(registry, npcId);

        NpcType playerType = znpcsPlus.getNpcTypeRegistry().getByName(NPCConstants.NPC_TYPE_PLAYER);
        if (playerType == null) {
            plugin.getLogger().warning(LogMessages.NPC_TYPE_NOT_FOUND);
            return;
        }

        NpcLocation npcLocation = new NpcLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        NpcEntry entry = registry.create(npcId, loc.getWorld(), playerType, npcLocation);
        entry.setProcessed(true);
        entry.setSave(false);

        Npc npc = entry.getNpc();
        npc.setEnabled(true);

        if (!data.texture.isEmpty() && !data.signature.isEmpty()) {
            applySkin(npc, data.texture, data.signature);
        }

        if (data.name != null && !data.name.isEmpty()) {
            npc.getHologram().insertLine(0, data.name);
        }
    }

    private void deleteExistingNpc(NpcRegistry registry, String npcId) {
        NpcEntry existingEntry = registry.getById(npcId);
        if (existingEntry != null) {
            registry.delete(npcId);
        }
    }

    private void handleDelete(short npcId) {
        NpcRegistry registry = znpcsPlus.getNpcRegistry();
        if (registry == null) {
            return;
        }

        String id = NPCConstants.NPC_ID_PREFIX + npcId;
        NpcEntry entry = registry.getById(id);
        if (entry != null) {
            registry.delete(id);
        }
    }

    private void handleUpdate(Serializer.NPCData data) {
        NpcRegistry registry = znpcsPlus.getNpcRegistry();
        if (registry == null) {
            return;
        }

        String npcId = NPCConstants.NPC_ID_PREFIX + data.npcId;
        NpcEntry entry = registry.getById(npcId);
        if (entry == null) {
            handleCreate(data);
            return;
        }

        Npc npc = entry.getNpc();
        Location loc = data.location.toLocation(Bukkit.getServer());
        if (loc != null) {
            NpcLocation npcLocation = new NpcLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            npc.setLocation(npcLocation);
        }

        if (!data.texture.isEmpty() && !data.signature.isEmpty()) {
            applySkin(npc, data.texture, data.signature);
        }
    }

    @SuppressWarnings("unchecked")
    private void applySkin(Npc npc, String texture, String signature) {
        SkinDescriptor skin = znpcsPlus.getSkinDescriptorFactory().createStaticDescriptor(texture, signature);
        EntityProperty<SkinDescriptor> skinProperty = (EntityProperty<SkinDescriptor>) znpcsPlus.getPropertyRegistry()
                .getByName(NPCConstants.SKIN_PROPERTY_NAME);
        if (skinProperty != null) {
            npc.setProperty(skinProperty, skin);
        }
    }
}

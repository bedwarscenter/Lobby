package center.bedwars.lobby.sync.serialization;

public class ChunkData {

    public int chunkX;
    public int chunkZ;
    public byte[] snapshotData;

    public ChunkData() {
    }

    public ChunkData(int x, int z, byte[] data) {
        this.chunkX = x;
        this.chunkZ = z;
        this.snapshotData = data;
    }
}

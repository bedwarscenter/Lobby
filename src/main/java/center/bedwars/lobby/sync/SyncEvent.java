package center.bedwars.lobby.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class SyncEvent {

    private final String sourceLobby;
    private final SyncEventType type;
    private final long timestamp;
    private final ByteBuf data;

    public SyncEvent(String sourceLobby, SyncEventType type, long timestamp, ByteBuf data) {
        this.sourceLobby = sourceLobby;
        this.type = type;
        this.timestamp = timestamp;
        this.data = data;
    }

    public SyncEvent(String sourceLobby, SyncEventType type, ByteBuf data) {
        this(sourceLobby, type, System.currentTimeMillis(), data);
    }

    public ByteBuf serialize() {
        ByteBuf buf = Unpooled.buffer();
        writeUTF(buf, sourceLobby);
        buf.writeByte(type.ordinal());
        buf.writeLong(timestamp);
        buf.writeBytes(data);
        return buf;
    }

    public static SyncEvent deserialize(ByteBuf buf) {
        String lobby = readUTF(buf);
        SyncEventType type = SyncEventType.fromOrdinal(buf.readByte());
        long time = buf.readLong();
        ByteBuf data = buf.slice(buf.readerIndex(), buf.readableBytes());
        return new SyncEvent(lobby, type, time, data);
    }

    public boolean isFromSameLobby(String currentLobbyId) {
        return sourceLobby != null && sourceLobby.equals(currentLobbyId);
    }

    public String getSourceLobby() {
        return sourceLobby;
    }

    public SyncEventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ByteBuf getData() {
        return data;
    }

    private static void writeUTF(ByteBuf buf, String s) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(b.length);
        buf.writeBytes(b);
    }

    private static String readUTF(ByteBuf buf) {
        int len = buf.readShort();
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }
}
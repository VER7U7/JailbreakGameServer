package com.VER7U7.Server.Network;

import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32;

public class NetworkPacket {
    private short packetId;
    private short playerID;
    private long timestamp;
    private long confirmHashCode;
    private byte[] data;

    public NetworkPacket(byte[] data, int packetId, short playerID, long timestamp, long confirmHashCode) {
        this.data = data;
        this.playerID = playerID;
        this.packetId = (short)packetId;
        this.timestamp = timestamp;
        this.confirmHashCode = confirmHashCode;
    }

    public NetworkPacket(byte[] data, int packetId, int playerId) {
        this(data, packetId, (short)playerId, System.currentTimeMillis(), 0);
        confirmHashCode = getCrcHash(data);
    }

    public NetworkPacket(byte[] data, int packetId) {
        this(data, packetId, (short)0, System.currentTimeMillis(), 0);
        confirmHashCode = getCrcHash(data);
    }

    public ByteBuffer getFormattedDataBuffer() { //4 byte header; 2 byte length; 2 byte supp; 2 byte packetId; 8 byte timestamp; n * size bytes data; 8 bytes hashCode;
        return LittleByteBuffer.allocate(4 + 2 + 2 + 2 + 8 + data.length + 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(NetworkConstants.NEJB_PROTOCOL_HEADER)
                .putShort((short)data.length)
                .putShort(packetId)
                .putShort(playerID)
                .putLong(timestamp)
                .put(data)
                .putLong(getCrcHash(data));
    }

    public byte[] getFormattedData() {
        return getFormattedDataBuffer().array();
    }

    public long getCrcHash(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public short getPacketId() {
        return packetId;
    }

    public NetworkPacket setPacketId(short packetId) {
        this.packetId = packetId;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public NetworkPacket setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public long getConfirmHashCode() {
        return confirmHashCode;
    }

    public NetworkPacket setConfirmHashCode(long confirmHashCode) {
        this.confirmHashCode = confirmHashCode;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public NetworkPacket setData(byte[] data) {
        this.data = data;
        return this;
    }

    public short getPlayerID() {
        return playerID;
    }

    public NetworkPacket setPlayerID(int playerID) {
        this.playerID = (short) playerID;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkPacket)) return false;
        NetworkPacket that = (NetworkPacket) o;
        return packetId == that.packetId && playerID == that.playerID && timestamp == that.timestamp && confirmHashCode == that.confirmHashCode && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(packetId, playerID, timestamp, confirmHashCode);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "NetworkPacket{" +
                "packetId=" + packetId +
                ", playerID=" + playerID +
                ", timestamp=" + timestamp +
                ", confirmHashCode=" + confirmHashCode +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}

package com.VER7U7.Server.Network;

import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32;

public class NetworkPacket {
    private short packetId;
    private int packetTransferID;
    private long timestamp;
    private long confirmHashCode;
    private byte[] data;

    public NetworkPacket(byte[] data, int packetId, int packetTransferID, long timestamp, long confirmHashCode) {
        this.data = data;
        this.packetTransferID = packetTransferID;
        this.packetId = (short)packetId;
        this.timestamp = timestamp;
        this.confirmHashCode = confirmHashCode;
    }

    public NetworkPacket(byte[] data, int packetId, int packetTransferID) {
        this(data, packetId, packetTransferID, System.currentTimeMillis(), 0);
        confirmHashCode = getCrcHash(data);
    }

    public NetworkPacket(byte[] data, int packetId) {
        this(data, packetId, 0, System.currentTimeMillis(), 0);
        confirmHashCode = getCrcHash(data);
    }

    public ByteBuffer getFormattedDataBuffer() { //4 byte header; 2 byte length; 2 byte packetId; 4 packetTransferId, 8 byte timestamp; n * size bytes data; 8 bytes hashCode;
        return LittleByteBuffer.allocate(4 + 2 + 2 + 4 + 8 + data.length + 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(NetworkConstants.NEJB_PROTOCOL_HEADER)
                .putShort((short)data.length)
                .putShort(packetId)
                .putInt(packetTransferID)
                .putLong(timestamp)
                .put(data)
                .putLong(getCrcHash(data)).flip();
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

    public int getPacketTransferID() {
        return packetTransferID;
    }

    public void setPacketTransferID(int packetTransferID) {
        this.packetTransferID = packetTransferID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkPacket)) return false;
        NetworkPacket that = (NetworkPacket) o;
        return packetId == that.packetId && packetTransferID == that.packetTransferID && timestamp == that.timestamp && confirmHashCode == that.confirmHashCode && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(packetId, packetTransferID, timestamp, confirmHashCode);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "NetworkPacket{" +
                "packetId=" + packetId +
                ", packetTransferID=" + packetTransferID +
                ", timestamp=" + timestamp +
                ", confirmHashCode=" + confirmHashCode +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}

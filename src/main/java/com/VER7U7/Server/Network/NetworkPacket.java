package com.VER7U7.Server.Network;

import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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

    public byte[] getFormattedData() { //4 byte header; 2 byte length; 2 byte supp; 2 byte packetId; 8 byte timestamp; n * size bytes data; 8 bytes hashCode;
        return LittleByteBuffer.allocate(4 + 2 + 2 + 2 + 8 + data.length + 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(NetworkConstants.NEJB_PROTOCOL_HEADER)
                .putShort((short)data.length)
                .putShort(packetId)
                .putShort(playerID)
                .putLong(timestamp)
                .put(data)
                .putLong(getCrcHash(data))
                .array();
    }

    public long getCrcHash(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public short getPacketId() {
        return packetId;
    }

    public void setPacketId(short packetId) {
        this.packetId = packetId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getConfirmHashCode() {
        return confirmHashCode;
    }

    public void setConfirmHashCode(long confirmHashCode) {
        this.confirmHashCode = confirmHashCode;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

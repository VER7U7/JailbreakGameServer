package com.VER7U7.UnityPhysics.JUPP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class JUPPPacket {
    private byte[] data;
    public short packetID;
    public int packetTransferID;


    public JUPPPacket(byte[] data, short packetID) {
        this.packetID = packetID;
        this.data = data;
    }

    public JUPPPacket(byte[] data, short packetID, int packetTransferID){
        this(data, packetID);
        this.packetTransferID = packetTransferID;
    }

    /* use for send packets */
    public byte[] getFormattedData() {
        if (packetTransferID <= 0 || packetID <= 0)
            return null;

        return ByteBuffer.allocate(data.length + 6)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(packetTransferID)
                .putShort(packetID)
                .put(data).array();
    }

    /* use for get data */
    public byte[] getData() {
        return data;
    }

    public short getPacketID() {
        return packetID;
    }

    public void setPacketID(short packetID) {
        this.packetID = packetID;
    }

    public int getPacketTransferID() {
        return packetTransferID;
    }

    public void setPacketTransferID(int packetTransferID) {
        this.packetTransferID = packetTransferID;
    }
}

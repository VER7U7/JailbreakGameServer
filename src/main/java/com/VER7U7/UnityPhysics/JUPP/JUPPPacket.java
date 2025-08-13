package com.VER7U7.UnityPhysics.JUPP;

import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class JUPPPacket {
    private byte[] data;
    public short packetID;
    public int packetTransferID;


    public JUPPPacket(byte[] data, short packetID) {
        this.packetID = packetID;
        this.data = data;
    }

    public JUPPPacket(byte[] data, int packetID) {
        this(data, (short)packetID);
    }

    public JUPPPacket(byte[] data, short packetID, int packetTransferID){
        this(data, packetID);
        this.packetTransferID = packetTransferID;
    }
    public JUPPPacket() {
        this(new byte[0], (short)0, 0);
    }

    /* use for send packets */
    public byte[] getFormattedData() {
        if (packetTransferID <= 0 || packetID <= 0)
            return null;

        return LittleByteBuffer.allocate(data.length + 6)
                .putInt(packetTransferID)
                .putShort(packetID)
                .put(data).array();
    }

    /* use for get data */
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JUPPPacket that = (JUPPPacket) o;
        return packetID == that.packetID && packetTransferID == that.packetTransferID && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(packetID, packetTransferID);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}

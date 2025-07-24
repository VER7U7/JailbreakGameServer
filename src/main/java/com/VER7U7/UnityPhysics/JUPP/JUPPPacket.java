package com.VER7U7.UnityPhysics.JUPP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class JUPPPacket {
    private boolean incoming;
    private byte[] data;
    private short length;



    public short packetID;

    private static final short packetIdSize = 2;


    public JUPPPacket(byte[] data, short length, boolean incoming, short offset) {
        this.incoming = incoming;
        if (this.incoming) {
            if (data.length < 6)
                return;

            this.length = ByteBuffer.wrap(data, offset + JUPPCommons.JUPP_HEADER.length(), 2).getShort();

            if (this.length > data.length) {
                this.length = 0;
                return;
            }

            this.data = new byte[this.length];
            System.arraycopy(data, offset + JUPPCommons.JUPP_HEADER.length() + 2 + packetIdSize, this.data, 0, this.length);
            packetID = ByteBuffer.wrap(data, JUPPCommons.JUPP_HEADER.length() + 3, 2).getShort();
        } else {
            this.length = length;
            this.data = new byte[length];
            System.arraycopy(data, offset, this.data, 0, length);
        }
    }

    public JUPPPacket(byte[] data, short length, boolean incoming){
        this(data, length, incoming, (short)0);
    }

    /* use for send packets */
    public byte[] getFormattedData() {
        return ByteBuffer.allocate(length + JUPPCommons.JUPP_HEADER.length() + 2 + packetIdSize)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(JUPPCommons.JUPP_HEADER.getBytes(StandardCharsets.US_ASCII))
                .putShort(length)
                .putShort(packetID)
                .put(data).array();
    }

    /* use for get data */
    public byte[] getData() {
        return data;
    }

    public short getLength() {
        return length;
    }
}

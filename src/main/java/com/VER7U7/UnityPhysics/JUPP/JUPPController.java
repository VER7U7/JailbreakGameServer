package com.VER7U7.UnityPhysics.JUPP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static com.VER7U7.UnityPhysics.JUPP.JUPPCommons.*;

public class JUPPController {

    private JUPPEngine engine;

    public JUPPController(JUPPEngine engine) {
        this.engine = engine;
    }

    /* DANGER!!!!!!!
    *  ALL OPERATIONS ARE CARRIED OUT IN LITTLE ENDIAN MODE
    * */

    public int createGameObject(String name) {
        byte[] nameData = name.getBytes(StandardCharsets.US_ASCII);
        byte[] data = ByteBuffer.allocate(nameData.length + 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short)nameData.length)
                .put(nameData, 0, nameData.length)
                .array();
        JUPPPacket packet = new JUPPPacket(data, JUPP_CREATE_OBJECT);
        JUPPPacket incomingPacket = engine.sendWithResult(packet);
        if (incomingPacket == null)
            return -1;
        return ByteBuffer.wrap(incomingPacket.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

}

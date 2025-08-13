package com.VER7U7.UnityPhysics.JUPP;

import com.VER7U7.Server.Utils.LittleByteBuffer;

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

    public boolean endTickSignal(long tickCounts) {
        byte[] data = LittleByteBuffer.allocate(8)
                .putLong(tickCounts)
                .array();
        JUPPPacket outPacket = new JUPPPacket(data, JuppOutgoingCommands.SyncEndTick.getID());
        JUPPPacket incomingPacket = engine.sendWithResult(outPacket);
        return true;
    }

    public int createGameObject(String name) {
        /*byte[] nameData = name.getBytes(StandardCharsets.US_ASCII);
        byte[] data = LittleByteBuffer.allocate(nameData.length + 2)
                .putShort((short)nameData.length)
                .put(nameData, 0, nameData.length)
                .array();
        JUPPPacket packet = new JUPPPacket(data, JUPP_CREATE_OBJECT);
        JUPPPacket incomingPacket = engine.sendWithResult(packet);
        if (incomingPacket == null)
            return -1;
        return LittleByteBuffer.wrap(incomingPacket.getData()).getInt();*/
        return 0;
    }

}

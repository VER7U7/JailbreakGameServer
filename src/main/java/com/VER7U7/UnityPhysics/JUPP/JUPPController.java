package com.VER7U7.UnityPhysics.JUPP;

import com.VER7U7.Server.Objects.JailPlayer;
import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.VER7U7.UnityPhysics.JUPP.JUPPCommons.*;

public class JUPPController {

    private JUPPEngine engine;

    public JUPPController(JUPPEngine engine) {
        this.engine = engine;
    }

    /* DANGER!!!!!!!
    *  ALL OPERATIONS ARE CARRIED OUT IN LITTLE ENDIAN MODE
    * */

    public boolean setupPools(short playerPoolSize) {
        byte[] data = LittleByteBuffer.allocate(2)
                .putShort(playerPoolSize)
                .array();
        JUPPPacket outPacket = new JUPPPacket(data, JuppOutgoingCommands.SetupPools.getID());
        JUPPPacket incomingPacket = engine.sendWithResult(outPacket);
        ByteBuffer buffer = LittleByteBuffer.wrap(incomingPacket.getData());
        JUPPLog.println("Pools initialized with: playerPool(" + buffer.getShort() + ")");
        return true;
    }

    public boolean playerUpdate(JailPlayer player, JailPlayer.PlayerUpdateType updateType) {
        if (updateType == JailPlayer.PlayerUpdateType.AddPlayer) {
            JUPPPacket outPacket = new JUPPPacket(player.addPlayerData(), JuppOutgoingCommands.UpdatePlayer.getID());
            JUPPPacket incomingPacket = engine.sendWithResult(outPacket);
            ByteBuffer buffer = LittleByteBuffer.wrap(incomingPacket.getData());
            player.unityInstanceID = buffer.getInt();
            if (player.unityInstanceID == -1) {
                JUPPLog.println("Error with adding player(" + player.playerID + ")");
                return false;
            }
            player.position.x = buffer.getFloat();
            player.position.y = buffer.getFloat();
            player.position.z = buffer.getFloat();
            return true;
        }
        if (updateType == JailPlayer.PlayerUpdateType.DeletePlayer) {
            JUPPPacket outPacket = new JUPPPacket(player.deletePlayerData(), JuppOutgoingCommands.UpdatePlayer.getID());
            JUPPPacket incomingPacket = engine.sendWithResult(outPacket);
            ByteBuffer buffer = LittleByteBuffer.wrap(incomingPacket.getData());
            boolean success = buffer.get() != 0;
            if (!success)
                return false;
            return true;
        }
        return false;
    }

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

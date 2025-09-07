package com.VER7U7.UnityPhysics.JUPP;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.VER7U7.Server.Packets.Data.IncomingPacketData.*;


import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import static com.VER7U7.UnityPhysics.JUPP.JUPPCommons.*;

public class JUPPController {
    private static final Logger LOGGER = LogManager.getLogger(JUPPController.class);


    private JUPPEngine engine;

    public JUPPController() {}

    public JUPPController setPhysicsEngine(JUPPEngine engine) {
        this.engine = engine;
        return this;
    }

    /* DANGER!!!!!!!
    *  ALL OPERATIONS ARE CARRIED OUT IN LITTLE ENDIAN MODE
    * */

    public boolean setupPools(short playerPoolSize) {
        try {
            byte[] data = LittleByteBuffer.allocate(2)
                    .putShort(playerPoolSize)
                    .array();
            JUPPPacket outPacket = new JUPPPacket(data, JuppOutgoingCommands.SetupPools.getID());
            JUPPPacket incomingPacket = engine.sendWithResultTimer(outPacket, 200);
            ByteBuffer buffer = LittleByteBuffer.wrap(incomingPacket.getData());
            LOGGER.debug("Pools initialized with: playerPool({})",  buffer.getShort());
        }catch(CancellationException | TimeoutException e) {
            return false;
        }
        return true;
    }

    public boolean playerUpdate(JailPlayer player, JailPlayer.PlayerUpdateType updateType) {
        try {
            if (updateType == JailPlayer.PlayerUpdateType.AddPlayer) {
                JUPPPacket outPacket = new JUPPPacket(player.addPlayerData(), JuppOutgoingCommands.UpdatePlayer.getID());
                JUPPPacket incomingPacket = engine.sendWithResult(outPacket);
                ByteBuffer buffer = LittleByteBuffer.wrap(incomingPacket.getData());
                player.unityInstanceID = buffer.getInt();
                if (player.unityInstanceID == -1) {
                    LOGGER.error("Error with adding player({})", player.playerID);
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
        }catch(CancellationException e) {
            return false;
        }
        return false;
    }

    public boolean inputClientSync(IncomingLocalInputSync origSyncPacket, JailPlayer player) {
        try {
            JUPPPacket outPacket = new JUPPPacket(player.clientInputSyncData(
                    origSyncPacket.cameraPos,
                    origSyncPacket.cameraRot,
                    origSyncPacket.offsetZ,
                    origSyncPacket.horizontal,
                    origSyncPacket.vertical,
                    origSyncPacket.jumpDown,
                    origSyncPacket.sprintDown),
                    JuppOutgoingCommands.UpdatePlayer.getID());

            JUPPPacket physicsAnswer = engine.sendWithResult(outPacket);
            ByteBuffer buffer = LittleByteBuffer.wrap(physicsAnswer.getData()); //status
            if (buffer.get() == 1) {
                return true;
            }
        }catch(CancellationException e) {
            return false;
        }
        return false;
    }

    public boolean playerSync(JailPlayer player) {
        try {
            JUPPPacket outPacket = new JUPPPacket(
                    player.playerSyncData(),
                    JuppOutgoingCommands.UpdatePlayer.getID());

            JUPPPacket physicsAnswer = engine.sendWithResult(outPacket);
            ByteBuffer buffer = LittleByteBuffer.wrap(physicsAnswer.getData());
            if (buffer.get() == 1) {
                player.position.fromBytes(buffer);
                player.velocity.fromBytes(buffer);
                player.rotation.fromBytes(buffer);
                player.jumpDelayTime = buffer.getFloat();
                player.isGrounded = buffer.get() != 0;

                player.cameraPosition.fromBytes(buffer);
                player.cameraRotation.fromBytes(buffer);
                player.cameraOffsetZ = buffer.getFloat();
                return true;
            }
        }catch (CancellationException e) {
            return false;
        }
        return false;
    }

    public boolean endTickSignal(long tickCounts) {
        try {
            byte[] data = LittleByteBuffer.allocate(8)
                    .putLong(tickCounts)
                    .array();
            JUPPPacket outPacket = new JUPPPacket(data, JuppOutgoingCommands.SyncEndTick.getID());
            JUPPPacket incomingPacket = engine.sendWithResult(outPacket);
        }catch (CancellationException e) {
            return false;
        }
        return true;
    }


}

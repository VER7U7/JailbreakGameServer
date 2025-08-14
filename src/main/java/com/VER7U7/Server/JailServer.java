package com.VER7U7.Server;

import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.States.NetworkIncomingMessage;
import com.VER7U7.Server.Network.States.NetworkOutgoingMessage;
import com.VER7U7.Server.ObjectFactories.JailPlayerFactory;
import com.VER7U7.Server.Objects.JailPlayer;
import com.VER7U7.Server.Packets.JailPacketService;
import com.VER7U7.Server.Utils.DeltaTime;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;
import com.VER7U7.UnityPhysics.JUPP.JUPPLog;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.VER7U7.Main.*;
import static com.VER7U7.Server.JailConstants.*;
import static com.VER7U7.Server.Packets.PacketConstants.*;

public class JailServer extends Thread {

    private JUPPController physicController;
    private JailPools jailPools;
    private JailPacketService jailPacketService;

    //Network
    private NetworkEngine playersNetwork;

    /* RUNTIME */
    public AtomicBoolean serverRunning = new AtomicBoolean(false);

    /* TICKS */
    private long ticksCount;
    private int actualTicks;
    private long lastTickTime = System.nanoTime();
    private long lastTickRateTime = System.nanoTime();

    public JailServer() {

    }

    public void StartSimulation() {
        serverRunning.set(true);
        try {
            jailPools = new JailPools().InitializePools();

            playersNetwork = new NetworkEngine(PLAYER_NETWORK_PORT).StartNetwork();
            playersNetwork.jailPools = jailPools;
            physicController = new JUPPController(physicsEngine);
            jailPacketService = new JailPacketService(this, physicController, playersNetwork, jailPools);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.start();
    }

    public boolean hasServerAvailable() {
        return serverRunning.get();
    }

    public void StopSimulation() {
        serverRunning.set(false);
        this.interrupt();
        playersNetwork.StopNetwork();
    }

    @Override
    public void run() {
        //initialize
        physicController.setupPools((short)SERVER_MAX_PLAYERS);

        try {
            while(true) {
                long now = System.nanoTime();
                long elapsedTime = now - lastTickTime;

                if (elapsedTime >= NS_PER_SERVER_TICK) {
                    lastTickTime += NS_PER_SERVER_TICK;

                    actualTicks++;
                    if (now - lastTickRateTime >= 1_000_000_000L) {
                        System.out.println("Actual Tick Rate: " + actualTicks + " ticks/sec");
                        actualTicks = 0;
                        lastTickRateTime = now;


                    }

                    //--- START TICK PROCESSING ---
                    ticksCount++;
                    processIncomingMessages();
                    physicController.endTickSignal(ticksCount);

                    if (System.nanoTime() - lastTickTime > NS_PER_SERVER_TICK) {
                        lastTickTime = System.nanoTime();
                    }
                } else {
                    long sleepTimeNs = NS_PER_SERVER_TICK - elapsedTime;
                    long sleepTimeMs = sleepTimeNs / 1_000_000;
                    int sleepTimeNsRemainder = (int) (sleepTimeMs % 1_000_000);
                    if (sleepTimeMs > 0) {
                        Thread.sleep(sleepTimeMs, sleepTimeNsRemainder);
                    }
                }

            }
        }catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    private void processIncomingMessages() {
        if (!playersNetwork.getNetworkAvailable())
            return;

        NetworkIncomingMessage incomingMessage;
        while ((incomingMessage  = playersNetwork.getIncomingMessages().poll()) != null) {
            try {
                if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_DEFAULT) {
                    IncomingPacketType packetType = IncomingPacketType.fromID(incomingMessage.getPacket().getPacketId());
                    jailPacketService.callToPacketFactory(packetType, incomingMessage.getPlayerID(), incomingMessage.getPacket());
                    continue;
                }

                if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_NEW_PLAYER) {
                    JailPlayer player = JailPlayerFactory.createPlayer(incomingMessage.getPlayerID())
                            .setNickName("Player " + incomingMessage.getPlayerID())
                            .build();
                    jailPools.playersPool.put((int)player.playerID, player);

                    if (!physicController.playerUpdate(player, JailPlayer.PlayerUpdateType.AddPlayer)) {
                        playersNetwork.addPacketToOutgoing(null, NetworkOutgoingMessage.NETWORK_DISCONNECT_PLAYER, player.playerID);
                    } else {
                        JUPPLog.println("Added " + player.nickname + " with instance(" + player.unityInstanceID +
                                ") at position(x:"+ player.position.x
                                +"; y: "+ player.position.y +"; z: " + player.position.z + ")");
                    }
                }

                if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_DELETE_PLAYER) {
                    JailPlayer player = jailPools.playersPool.get(incomingMessage.getPlayerID());
                    jailPools.DeletePlayer(player.playerID);
                    physicController.playerUpdate(player, JailPlayer.PlayerUpdateType.DeletePlayer);
                    JUPPLog.println("Removed " + player.nickname + " from the server.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

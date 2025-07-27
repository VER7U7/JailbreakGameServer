package com.VER7U7.Server;

import com.VER7U7.Main;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Network.States.NetworkIncomingMessage;
import com.VER7U7.Server.Network.States.NetworkPlayerSession;
import com.VER7U7.Server.Packets.JailPacketService;
import com.VER7U7.Server.Utils.DeltaTime;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
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
    private DeltaTime deltaTime;
    private long ticksCount;
    private long lastPoint;

    public JailServer() {

    }

    public void StartSimulation() {
        serverRunning.set(true);
        try {
            deltaTime = new DeltaTime();
            playersNetwork = new NetworkEngine(PLAYER_NETWORK_PORT).StartNetwork();
            jailPools = new JailPools().InitializePools();
            //physicController = new JUPPController(physicsEngine);
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

        try {
            while(true) {
                deltaTime.registerStart();

                //update packets

                if (ticksCount - lastPoint > 128) {
                    //System.out.println(physicController.createGameObject(new Random().nextInt(Integer.MAX_VALUE) + ""));
                    lastPoint = ticksCount;
                }


                ticksCount++;
                deltaTime.registerEnd();
                sleep(1024 / SERVER_TICK_RATE -
                        (deltaTime.getDeltaTimeMillis() > (1024 / SERVER_TICK_RATE) ?
                                (1024 / SERVER_TICK_RATE) :
                                deltaTime.getDeltaTimeMillis()
                        ));
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
            if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_DEFAULT) {

                continue;
            }

        }
    }

}

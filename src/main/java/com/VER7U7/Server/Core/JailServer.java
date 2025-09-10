package com.VER7U7.Server.Core;

import com.VER7U7.Server.Gameplay.Rules.BasicRules;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.States.NetworkIncomingMessage;
import com.VER7U7.Server.Network.States.NetworkOutgoingMessage;
import com.VER7U7.Server.Gameplay.EntityFactories.JailPlayerFactory;
import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.Server.Packets.Handlers.FunctionGlobalArgs;
import com.VER7U7.Server.Packets.Services.JailPacketService;
import com.VER7U7.UnityPhysics.JUPP.JUPPCommons;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;
import com.VER7U7.UnityPhysics.JUPP.JUPPEngine;
import com.VER7U7.UnityPhysics.JUPP.JUPPExceptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.VER7U7.Main.*;
import static com.VER7U7.Server.Core.JailConstants.*;
import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;
import static com.VER7U7.Server.Packets.Data.OutgoingPacketData.*;

public class JailServer {
    private static final Logger LOGGER = LogManager.getLogger(JailServer.class);

    //JUPP
    private JUPPEngine physicsEngine;
    private JUPPController physicController;
    private JailPhysicsStatus physicsStatus = JailPhysicsStatus.NeedInitializePhysics;

    //CORE
    private JailPools jailPools;
    private JailPacketService jailPacketService;

    //Network
    private NetworkEngine playersNetwork;

    /* RUNTIME */
    private Thread processThread;
    public AtomicBoolean serverRunning = new AtomicBoolean(false);

    /* TICKS */
    private long ticksCount;
    private int actualTicks;
    private long lastTickTime = System.nanoTime();
    private long lastTickRateTime = System.nanoTime();

    public JailServer() throws IOException {
        LOGGER.info("Initializing server {}", SERVER_VERSION_TEXT);
        Initialize();
    }

    public void Initialize() throws IOException {
        jailPools = new JailPools().InitializePools();
        playersNetwork = new NetworkEngine(PLAYER_NETWORK_PORT).StartNetwork(jailPools);
        physicController = new JUPPController();
        jailPacketService = new JailPacketService(new FunctionGlobalArgs(this, physicController, playersNetwork, jailPools));
    }

    public void InitializePhysics() throws JUPPExceptions.PhysicsError {
        if (!physicController.setupPools((short)SERVER_MAX_PLAYERS))
            throw new JUPPExceptions.PhysicsError("Setup pools error");
    }

    public void StartSimulation() throws JUPPExceptions.VersionNotMatch {
        serverRunning.set(true);

        physicsEngine = new JUPPEngine(JUBB_NETWORK_PORT, () -> { restartCallback(); });
        physicsEngine.Start();
        physicController.setPhysicsEngine(physicsEngine);

        processThread = new Thread(this::run, "JailServerThread");
        processThread.start();
        LOGGER.info("Server has been started {}", SERVER_VERSION_TEXT);
    }

    public boolean hasServerAvailable() {
        return serverRunning.get();
    }

    public void StopSimulation() {
        serverRunning.set(false);

        processThread.interrupt();
        processThread = null;

        if (playersNetwork != null) {
            playersNetwork.DisableShutdownHook();
            playersNetwork.StopNetwork();
        }
    }

    public void restartCallback() {
        if (serverRunning.get()) {
            LOGGER.warn("Restarting physics server");
            physicsStatus = JailPhysicsStatus.NeedInitializePhysics;
            try {
                if (playersNetwork != null) {
                    playersNetwork.DisableShutdownHook();
                    playersNetwork.StopNetwork();
                }

                Initialize();
            }catch(IOException e) {
                LOGGER.fatal("Server cannot be restarted", e);
            }
        }
    }

    public void run() {
        try {
            while(serverRunning.get()) {
                if (physicsStatus == JailPhysicsStatus.NeedInitializePhysics &&
                    physicsEngine.getBridgeStatus() == JUPPCommons.BridgeStatus.BridgeConnected) {
                    try {
                        InitializePhysics();
                    }catch(JUPPExceptions.PhysicsError e) {
                        LOGGER.fatal("Physics cannot be initialized by: {}", e.getMessage());
                        return;
                    }
                    physicsStatus = JailPhysicsStatus.PhysicsInitialized;
                }

                if (physicsEngine.getBridgeStatus() == JUPPCommons.BridgeStatus.BridgeStarted) {
                    Thread.sleep(1000);
                    continue;
                }

                process(); //base logic of server
            }
        }catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /*
    * this function must be in loop, because its main server logic.
    * in the function such process occur as: Ticks, Process Incoming Messages from
    * network engine (packets from players), Send data to players for sync with server,
    * Send updates to physics server etc. There is also a tickrate delay compensation system
    * */
    private void process() throws InterruptedException {
        long now = System.nanoTime();
        long elapsedTime = now - lastTickTime;

        if (elapsedTime >= NS_PER_SERVER_TICK) {
            lastTickTime += NS_PER_SERVER_TICK;

            actualTicks++;
            if (now - lastTickRateTime >= 1_000_000_000L) {
                LOGGER.debug("Actual Tick Rate: {} ticks/sec", actualTicks);
                actualTicks = 0;
                lastTickRateTime = now;
            }

            //--- START TICK PROCESSING ---
            ticksCount++;

            processIncomingMessages();
            sendAuthoritativeData();

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

    private void sendAuthoritativeData() {
        //Sync playing data (local player)
        for (Map.Entry<Integer, JailPlayer> player : jailPools.playersPool.entrySet()) {
            JailPlayer jailPlayer = player.getValue();

            if (jailPlayer.ticksTimeoutToSync > 0) {
                jailPlayer.ticksTimeoutToSync--;
                continue;
            }

            if (jailPlayer.state != JailPlayer.PlayerState.PlayerAlive &&
                jailPlayer.unityInstanceID != 0)
                continue;

            physicController.playerSync(jailPlayer);
            OutgoingPlayerSyncPacket outgoingPacket = new OutgoingPlayerSyncPacket(
                    jailPlayer.position, jailPlayer.velocity, jailPlayer.rotation, jailPlayer.jumpDelayTime, jailPlayer.isGrounded
            );
            playersNetwork.addPacketToOutgoing(outgoingPacket.Serialize(), 0, player.getKey());
        }

        //Sync deleted players
        if (jailPools.deletedPlayersPool.size() != 0) {
            OutgoingDeletePlayerPacket deletePlayerPacket = new OutgoingDeletePlayerPacket();
            for (JailPlayer deletePlayer : jailPools.deletedPlayersPool)
                deletePlayerPacket.allDeletePlayers.add(deletePlayer.playerID);

            jailPools.deletedPlayersPool.clear();

            for (JailPlayer receivePlayer : jailPools.playersPool.values()) {
                playersNetwork.addPacketToOutgoing(deletePlayerPacket.Serialize(), 0, receivePlayer.playerID);
            }
        }

        //Sync added players
        if (jailPools.addedPlayersPool.size() != 0) {
            OutgoingAddPlayerPacket addedPlayerPacket = new OutgoingAddPlayerPacket();
            for (JailPlayer addedPlayer : jailPools.addedPlayersPool) {
                addedPlayerPacket.addPlayersInfo.put(addedPlayer.playerID, addedPlayer);
            }

            jailPools.addedPlayersPool.clear();

            for (JailPlayer receivePlayer : jailPools.playersPool.values()) {
                playersNetwork.addPacketToOutgoing(addedPlayerPacket.Serialize(), 0, receivePlayer.playerID);
            }
        }

        //Sync all players
        if (jailPools.playersPool.size() != 0) {
            OutgoingAllPlayersInfoPacket allPlayersInfoPacket = new OutgoingAllPlayersInfoPacket();
            for (JailPlayer eachPlayer : jailPools.playersPool.values()) {
                allPlayersInfoPacket.playersInfo.put(eachPlayer.playerID, eachPlayer);
            }

            for (JailPlayer receivePlayer : jailPools.playersPool.values()) {
                playersNetwork.addPacketToOutgoing(allPlayersInfoPacket.Serialize(), 0, receivePlayer.playerID);
            }
        }
    }


    private void processIncomingMessages() {
        if (!playersNetwork.getNetworkAvailable())
            return;

        List<NetworkIncomingMessage> messagesBatch = new ArrayList<>(); //push all packets in 1 container
        NetworkIncomingMessage batchMessage;
        while((batchMessage = playersNetwork.getIncomingMessages().poll()) != null) {
            messagesBatch.add(batchMessage);
        }

        for (NetworkIncomingMessage incomingMessage : messagesBatch) { //process packets
            try {
                if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_DEFAULT) {
                    IncomingPacketType packetType = IncomingPacketType.fromID(incomingMessage.getPacket().getPacketId());
                    jailPacketService.callToPacketFactory(packetType, incomingMessage.getPlayerID(), incomingMessage.getPacket());
                    continue;
                }

                if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_NEW_PLAYER) {
                    LOGGER.debug("Initializing new player....");
                    JailPlayer player = JailPlayerFactory.createPlayer(incomingMessage.getPlayerID())
                            .setNickName("Player " + incomingMessage.getPlayerID())
                            .setPlayingTeam(BasicRules.Team.Spectator)
                            .build();
                    jailPools.AddPlayer(player); //Initialize player at pools

                    player.state = JailPlayer.PlayerState.Spectator; //Move player to spectators
                    playersNetwork.addPacketToOutgoing(
                            new OutgoingSpectatorPacket(0).Serialize(), 0, player.playerID
                    );

                    Map<Short, JailPlayer> allPlayersToAdd = new HashMap<>(); //Adding players to scene at client
                    for (Map.Entry<Integer, JailPlayer> players : jailPools.playersPool.entrySet()) {
                        LOGGER.debug("{} : {}", players.getValue().playerID, player.playerID);
                        allPlayersToAdd.put(players.getKey().shortValue(), players.getValue());
                    }
                    playersNetwork.addPacketToOutgoing(
                            new OutgoingAddPlayerPacket(allPlayersToAdd).Serialize(), 0, player.playerID
                    );

                    if (!physicController.playerUpdate(player, JailPlayer.PlayerUpdateType.AddPlayer)) { //Add player to physics
                        playersNetwork.addPacketToOutgoing(null, NetworkOutgoingMessage.NETWORK_DISCONNECT_PLAYER, player.playerID);
                    } else {
                        LOGGER.debug("Added " + player.nickname + " with instance(" + player.unityInstanceID +
                                ") at position(x:"+ player.position.x
                                +"; y: "+ player.position.y +"; z: " + player.position.z + ")");

                        player.state = JailPlayer.PlayerState.PlayerAlive;
                        player.ticksTimeoutToSync = 1;
                        player.playingTeam = BasicRules.Team.Prisoner;
                        LOGGER.debug("Send player spawn packet");
                        playersNetwork.addPacketToOutgoing( //Spawn player at client and move to team;
                                new OutgoingSpawnPacket(BasicRules.Team.Prisoner.getID(),
                                        player.position,
                                        player.rotation
                                        ).Serialize(), 0, player.playerID
                        );
                    }
                }

                if (incomingMessage.getMessageType() == NetworkIncomingMessage.NETWORK_INCOMING_DELETE_PLAYER) {
                    JailPlayer player = jailPools.playersPool.get(incomingMessage.getPlayerID());
                    jailPools.DeletePlayer(player.playerID);
                    physicController.playerUpdate(player, JailPlayer.PlayerUpdateType.DeletePlayer);
                    LOGGER.debug("Removed " + player.nickname + " from the server.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

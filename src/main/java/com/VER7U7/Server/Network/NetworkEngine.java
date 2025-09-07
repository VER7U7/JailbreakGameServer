package com.VER7U7.Server.Network;

import com.VER7U7.Server.Core.JailPools;
import com.VER7U7.Server.Network.Exceptions.IllegalPacketFormatException;
import com.VER7U7.Server.Network.States.*;
import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;
import com.VER7U7.UnityPhysics.JUPP.JUPPEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.VER7U7.Server.Core.JailConstants.*;
import static com.VER7U7.Server.Network.NetworkConstants.*;
import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;
import static com.VER7U7.Server.Packets.Data.OutgoingPacketData.*;
import static com.VER7U7.Server.Packets.Data.IncomingPacketData.*;

public class NetworkEngine extends Thread {
    private static final Logger LOGGER = LogManager.getLogger(NetworkEngine.class);


    private final int port;
    private Selector selector;
    private DatagramChannel channel;
    private final Random random = new Random();

    private final ConcurrentMap<SocketAddress, NetworkPlayerSession> addressToSession;
    private final ConcurrentMap<Integer, NetworkPlayerSession> playerIdToSession;
    private final ConcurrentMap<SocketAddress, ConnectionData> nonAuthedConnections;
    private final ConcurrentLinkedQueue<NetworkIncomingMessage> incomingQueue;
    private final ConcurrentLinkedQueue<NetworkOutgoingMessage> outgoingQueue;
    private final ConcurrentMap<Integer, NetworkWaitConfirmMessage> waitConfirmationPackets;
    private final ConcurrentMap<SocketAddress, NetworkConfirmData> confirmedPacketsQueue;

    public AtomicBoolean networkReady = new AtomicBoolean(false);
    private Runnable callbackSessionTimeout;
    private Thread shutdownCallback;
    private JailPools jailPools;

    public NetworkEngine(int port) {
        this.port = port;

        this.addressToSession = new ConcurrentHashMap<>();
        this.playerIdToSession = new ConcurrentHashMap<>();
        this.nonAuthedConnections = new ConcurrentHashMap<>();
        this.waitConfirmationPackets = new ConcurrentHashMap<>();
        this.confirmedPacketsQueue = new ConcurrentHashMap<>();

        this.incomingQueue = new ConcurrentLinkedQueue<>();
        this.outgoingQueue = new ConcurrentLinkedQueue<>();

        this.setName("NetworkEngineThread");

        LOGGER.info("UDP Server started on port {}", port);
    }

    public NetworkEngine StartNetwork(JailPools jailPools) throws IOException {
        this.jailPools = jailPools;

        this.selector = Selector.open();
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.bind(new InetSocketAddress(port));
        this.channel.register(selector, SelectionKey.OP_READ);

        this.start();
        networkReady.set(true);

        shutdownCallback = new Thread(this::StopNetwork);
        Runtime.getRuntime().addShutdownHook(shutdownCallback);
        return this;
    }

    public boolean getNetworkAvailable() {
        return networkReady.get();
    }

    public void DisableShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(shutdownCallback);
    }

    public void StopNetwork() {
        this.networkReady.set(false);

        LOGGER.warn("Network shutdown");
        try {
            if (networkReady.get()) {
                OutgoingDisconnect disconnectPacket = new OutgoingDisconnect(DisconnectReason.ClientDisconnect);
                NetworkPacket outgoingPacket = disconnectPacket.Serialize();
                for (Map.Entry<Integer, NetworkPlayerSession> entry : playerIdToSession.entrySet()) {
                    addPacketToOutgoing(outgoingPacket, 0, entry.getKey());
                    channel.send(disconnectPacket.Serialize().getFormattedDataBuffer(), entry.getValue().getClientAddress());
                    disconnectPlayer(entry.getValue().getClientAddress(), DisconnectReason.ClientDisconnect);
                }
            }
        }catch(IOException e) {
            e.printStackTrace();
        }

        try {
            channel.close();
            selector.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
        this.interrupt();
    }


    public void bindCallbackSessionTimeout(Runnable callback) {
        this.callbackSessionTimeout = callback;
    }

    public ConcurrentLinkedQueue<NetworkIncomingMessage> getIncomingMessages() {
        return incomingQueue;
    }

    public ConcurrentLinkedQueue<NetworkOutgoingMessage> getOutgoingMessages() {
        return outgoingQueue;
    }


    public void addPacketToOutgoing(NetworkPacket packet, int messageType, int playerId) {
        NetworkOutgoingMessage outgoingMessage = new NetworkOutgoingMessage(packet, messageType, playerId);
        outgoingQueue.add(outgoingMessage);
        selector.wakeup();
    }

    //public void addPacketToOutgoing Network

    @Override
    public void run() {
        ByteBuffer receiveBuffer = LittleByteBuffer.allocate(NEJB_PROTOCOL_MAX_LENGTH);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processOutgoingMessages();

                selector.select(10);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid())
                        continue;

                    if (key.isReadable())
                        handleRead(receiveBuffer);
                }

                checkSessionTimeouts();
                processWaitConfirmationPackets();
            }
            catch (ClosedSelectorException ignore) { }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        networkReady.set(false);
    }


    private void handleRead(ByteBuffer buffer) {
        buffer.clear();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            SocketAddress senderAddress = channel.receive(buffer);

            if (senderAddress != null) {
                buffer.flip();

                NetworkPlayerSession session = addressToSession.get(senderAddress);
                int networkIncomingType = NetworkIncomingMessage.NETWORK_INCOMING_DEFAULT;

                NetworkPacket packet;
                try {
                    packet = parsePacket(buffer);

                    if (updateReceiptConfirmation(packet, senderAddress))
                        return;

                    if (session == null) {
                        handleHandshake(senderAddress, packet);
                        return;
                    }
                    session.updateLastSeenTimestamp();

                    if (updateServicePackets(packet, session))
                        return;

                    if (updatePing(packet, session))
                        return;
                }catch (IllegalPacketFormatException e) {
                    return;
                }

                incomingQueue.add(new NetworkIncomingMessage(packet, networkIncomingType, session.getPlayerID()));
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * RTT calculates in 3 steps. First packet client sent to server with 'step == 0'
     * server save askSendTime and send second packet to client with 'step == 1', client calculate
     * RTT and send last packet with 'step == 2' (its last iteration) and server calculate RTT.
     * @return return true if this packet is ask {@code ID 7}
    * */
    public boolean updatePing(NetworkPacket packet, NetworkPlayerSession session) {
        if (packet.getPacketId() == IncomingPacketType.Ping.getID()) {
            JailPlayer player = jailPools.playersPool.get(session.getPlayerID());
            IncomingPing pingPacket = new IncomingPing();
            pingPacket.Deserialize(packet);

            if (pingPacket.step == 0) {
                OutgoingPing outgoingAsk = new OutgoingPing((byte) 1, System.currentTimeMillis());
                addPacketToOutgoing(outgoingAsk.Serialize(), 0, session.getPlayerID());
                player.askSendTime = System.currentTimeMillis();
            } else if (pingPacket.step == 2) {
                long newRTT = System.currentTimeMillis() - player.askSendTime;
                if (player.RTT == 0)
                    player.RTT = (int)newRTT;
                else
                    player.RTT = (int)((player.RTT * 9 + newRTT) / 10);
            }
            return true;
        }
        return false;
    }

    public boolean updateServicePackets(NetworkPacket packet, NetworkPlayerSession session) {
        if (packet.getPacketId() == IncomingPacketType.Disconnect.getID()) {
            IncomingDisconnect disconnectPacket = new IncomingDisconnect(packet);
            disconnectPlayer(session.getClientAddress(), disconnectPacket.disconnectReason);
            LOGGER.info("Player disconnected by reason: {}", disconnectPacket.disconnectReason.getText());
            return true;
        }
        return false;
    }


    /**
     * Confirmation of packet acceptance occurs in several stages.
     * Stage 1 - the client sends a packet and if the system specifies that it needs to be confirmed,
     * it is added to the packet confirmation processing stream and waits for confirmation.
     * Stage 2 - After receiving it by the server, the server adds it to the confirmation pool
     * and sends it to the server. (The confirmation pool is cleared either
     * if 10 ms have passed or if there are more than 5 packets in the pool).
     * Stage 3 - the client receives the confirmation packet and removes the transfer IT from the confirmation pools.
     * If no response has been received after some time, the packet is resent and the loss counter is incremented.
     * The timing after which the packet is considered lost varies from RTT * 2 (When the application starts,
     * RTT = 2000 so that there is no spam with connection packets, after RTT is calculated, it will be used further).
     * @return return true if this packet is confirmAsk {@code ID 8}
     * */
    public boolean updateReceiptConfirmation(NetworkPacket packet, SocketAddress address) {
        if (packet.getPacketId() == IncomingPacketType.ConfirmASK.getID()) {
            IncomingConfirmAsk incomingConfirm = new IncomingConfirmAsk();
            incomingConfirm.Deserialize(packet);

            for (int transferID : incomingConfirm.transfers) {
                NetworkWaitConfirmMessage waitMessage = waitConfirmationPackets.get(transferID);
                if (waitMessage == null)
                    continue;

                if (waitMessage.getClientAddress() != address)
                    continue;

                waitConfirmationPackets.remove(transferID);
            }
            return true;
        } else {
            IncomingPacketType incoming = IncomingPacketType.fromID(packet.getPacketId());
            if (incoming.hasNeedConfirm()) {
                NetworkConfirmData userConfirmData;
                if (!confirmedPacketsQueue.containsKey(address)) {
                    userConfirmData = new NetworkConfirmData();
                    userConfirmData.setLastConfirmPacketTime(System.currentTimeMillis());
                    confirmedPacketsQueue.put(address, userConfirmData);
                } else {
                    userConfirmData = confirmedPacketsQueue.get(address);
                }

                userConfirmData.setLastConfirmPacketTime(System.currentTimeMillis());

                if (userConfirmData.getTransfers().size() < 1) {
                    userConfirmData.getTransfers().add(packet.getPacketTransferID());
                    userConfirmData.setFirstConfirmAddTime(System.currentTimeMillis());
                } else
                    userConfirmData.getTransfers().add(packet.getPacketTransferID());
            }
        }
        return false;
    }

    public void handleHandshake(SocketAddress socketAddress, NetworkPacket packet) throws IOException {

        if (!nonAuthedConnections.containsKey(socketAddress)) {
            if (packet.getPacketId() == IncomingPacketType.NewConnection.getID()) {
                int clientVersion = LittleByteBuffer.wrap(packet.getData()).getInt();
                if (clientVersion == SERVER_VERSION) {
                    nonAuthedConnections.put(socketAddress, new ConnectionData().setState(ConnectionData.ConnectionStateMachine.VERSION_CHECKED));
                    nonAuthedConnections.get(socketAddress).updateTimestamp();

                    OutgoingConnectionSendInvitationCode outgoing = new OutgoingConnectionSendInvitationCode(SERVER_VERSION);
                    channel.send(outgoing.Serialize().getFormattedDataBuffer(), socketAddress);
                } else {
                    OutgoingDisconnect outgoing = new OutgoingDisconnect(DisconnectReason.VersionNotMatch);
                    channel.send(outgoing.Serialize().getFormattedDataBuffer(), socketAddress);
                }
            } else {
                OutgoingDisconnect outgoing = new OutgoingDisconnect(DisconnectReason.WrongOrder);
                channel.send(outgoing.Serialize().getFormattedDataBuffer(), socketAddress);
            }
        } else {
            if (packet.getPacketId() == IncomingPacketType.ConnectionInvitationCode.getID() &&
                    nonAuthedConnections.get(socketAddress).getState() == ConnectionData.ConnectionStateMachine.VERSION_CHECKED) {

                String invitationCode = new String(packet.getData(), StandardCharsets.UTF_8);
                if (invitationCode.equals("JAILBREAK")) {
                    nonAuthedConnections.get(socketAddress).setState(ConnectionData.ConnectionStateMachine.INVITATION_CHECKED);
                    nonAuthedConnections.get(socketAddress).updateTimestamp();

                    int newPlayerId = calculateAvailablePlayerID();
                    NetworkPlayerSession session = new NetworkPlayerSession(socketAddress, newPlayerId);
                    playerIdToSession.put(newPlayerId, session);
                    addressToSession.put(socketAddress, session);

                    incomingQueue.add(new NetworkIncomingMessage(null, NetworkIncomingMessage.NETWORK_INCOMING_NEW_PLAYER, newPlayerId));

                    nonAuthedConnections.remove(socketAddress);

                    OutgoingConnectionSuccess outgoingSuccess = new OutgoingConnectionSuccess(newPlayerId);
                    channel.send(outgoingSuccess.Serialize().getFormattedDataBuffer(), socketAddress);
                    LOGGER.info("Connected new player to the server ({})", socketAddress);
                } else {
                    OutgoingDisconnect outgoing = new OutgoingDisconnect(DisconnectReason.WrongCode);
                    channel.send(outgoing.Serialize().getFormattedDataBuffer(), socketAddress);
                }
            }
        }

    }

    private int calculateAvailablePlayerID() {
        int id = 0;
        while (playerIdToSession.containsKey(id)) {
            id++;
        }
        return id;
    }

    private NetworkPacket parsePacket(ByteBuffer buffer) throws IllegalPacketFormatException {
        if (buffer.limit() < 26)
            throw new IllegalPacketFormatException("The pocket size is less than the allowed limit (26 service bytes).");

        /*System.out.println(buffer.limit());
        byte[] da = buffer.array();
        for (byte b : da) {
            int f = b & 0xff;
            System.out.print(f + " ");
        }
        System.out.println();*/

        byte[] headerBuffer = new byte[4];
        short dataLength = buffer.get(headerBuffer, 0, 4).getShort();
        if (!Arrays.equals(headerBuffer, NEJB_PROTOCOL_HEADER))
            throw new IllegalPacketFormatException("Protocol header does not match.");

        short packetID = buffer.getShort();
        int packetTransferId = buffer.getInt();
        long timestamp = buffer.getLong();
        byte[] data = new byte[dataLength];
        buffer.get(data, 0, dataLength);
        long commitHash = buffer.getLong();

        NetworkPacket packet = new NetworkPacket(data, packetID, packetTransferId, timestamp, commitHash);
        if (packet.getConfirmHashCode() != packet.getCrcHash(data))
            throw new IllegalPacketFormatException("CRC hash code was corrupted.");

        return packet;
    }

    private void processOutgoingMessages() {
        NetworkOutgoingMessage outgoingMessage;
        while ((outgoingMessage = outgoingQueue.poll()) != null) {

            if (outgoingMessage.getMessageType() == NetworkOutgoingMessage.NETWORK_OUTGOING_DEFAULT) {
                NetworkPlayerSession session = playerIdToSession.get(outgoingMessage.getPlayerID());
                if (session != null) {
                    try {

                        OutgoingPacketType checkConfirm = OutgoingPacketType.fromID(outgoingMessage.getPacket().getPacketId());
                        if (checkConfirm.hasNeedConfirm()) {
                            NetworkPacket packet = outgoingMessage.getPacket();
                            int transferID = 1;
                            while(waitConfirmationPackets.containsKey(transferID)) {
                                transferID = random.nextInt(Integer.MAX_VALUE);
                            }
                            packet.setPacketTransferID(transferID);
                            waitConfirmationPackets.put(transferID, new NetworkWaitConfirmMessage(packet, session.getClientAddress(), System.currentTimeMillis()));
                        }
                        channel.send(outgoingMessage.getPacket().getFormattedDataBuffer(), session.getClientAddress());
                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.error("Unknown session, may be client break connection.");
                }
            } else if (outgoingMessage.getMessageType() == NetworkOutgoingMessage.NETWORK_DISCONNECT_PLAYER) {
                NetworkPlayerSession session = playerIdToSession.get(outgoingMessage.getPlayerID());
                if (session != null) {
                    disconnectPlayer(session.getClientAddress(), DisconnectReason.InnerException);
                } else {
                    LOGGER.error("Unknown session, may be client break connection.");
                }
            }
        }
    }

    private void processWaitConfirmationPackets() {

        Iterator<Map.Entry<Integer, NetworkWaitConfirmMessage>> iterator = waitConfirmationPackets.entrySet().iterator();
        long currentTime = System.currentTimeMillis();

        while (iterator.hasNext()) {
            Map.Entry<Integer, NetworkWaitConfirmMessage> entry = iterator.next();
            var waitMessage = entry.getValue();

            try {
                if (waitMessage.isNotAuth()) {
                    if (!nonAuthedConnections.containsKey(waitMessage.getClientAddress()))
                        iterator.remove();

                    if ((currentTime - waitMessage.getLastTryTimestamp()) > NEJB_CONFIRM_NOT_LOGIN_MS) {
                        waitMessage.setLastTryTimestamp(currentTime);
                        channel.send(waitMessage.getPacket().getFormattedDataBuffer(), waitMessage.getClientAddress());
                    }
                } else {
                    int playerId = addressToPlayerID(waitMessage.getClientAddress());
                    if (playerId == -1) {
                        iterator.remove();
                        return;
                    }

                    if (!jailPools.playersPool.containsKey(playerId)) {
                        iterator.remove();
                        return;
                    }

                    int RTT = jailPools.playersPool.get(playerId).RTT + 8;
                    if (RTT > NEJB_PLAYER_TIMEOUT_MS)
                        RTT = NEJB_CONFIRM_NOT_LOGIN_MS;

                    int confirmDelay = Math.max(RTT * 2, NEJB_CONFIRM_LOGIN_MS);

                    if ((currentTime - waitMessage.getLastTryTimestamp()) > confirmDelay) {
                        waitMessage.setLastTryTimestamp(currentTime);
                        channel.send(waitMessage.getPacket().getFormattedDataBuffer(), waitMessage.getClientAddress());
                    }
                }
            }catch(IOException e) {
                e.printStackTrace();
            }
        }

        Iterator<Map.Entry<SocketAddress, NetworkConfirmData>> confirmedIterator = confirmedPacketsQueue.entrySet().iterator();
        while (confirmedIterator.hasNext()) {
            Map.Entry<SocketAddress, NetworkConfirmData> entry = confirmedIterator.next();
            SocketAddress address = entry.getKey();
            NetworkConfirmData confirmData = entry.getValue();

            if (System.currentTimeMillis() - confirmData.getLastConfirmPacketTime() > NEJB_PLAYER_TIMEOUT_MS) {
                confirmedIterator.remove();
                continue;
            }

            if (
                    (currentTime - confirmData.getFirstConfirmAddTime() > 10 &&
                    confirmData.getTransfers().size() > 0 ) ||
                    confirmData.getTransfers().size() > 5
            ) {
                try {
                    OutgoingConfirmAsk confirmAsk = new OutgoingConfirmAsk(new int[confirmData.getTransfers().size()]);
                    for (int i = 0; i < confirmAsk.transfers.length; i++) {
                        Integer transfer = confirmData.getTransfers().poll();
                        if (transfer == null)
                            continue;
                        confirmAsk.transfers[i] = transfer;
                    }
                    channel.send(confirmAsk.Serialize().getFormattedDataBuffer(), address);
                }catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int addressToPlayerID(SocketAddress address) {
        for (Map.Entry<Integer, NetworkPlayerSession> entry : playerIdToSession.entrySet()) {
            if (entry.getValue().getClientAddress() == address)
                return entry.getKey();
        }
        return -1;
    }

    private void checkSessionTimeouts() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<SocketAddress, NetworkPlayerSession>> iterator = addressToSession.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<SocketAddress, NetworkPlayerSession> entry = iterator.next();
            NetworkPlayerSession session = entry.getValue();
            if (currentTime - session.getLastSeenTimestamp() > NEJB_PLAYER_TIMEOUT_MS) {
                LOGGER.debug("Session " + session.getClientAddress() + " has been closed by timeout.");
                //iterator.remove();
                disconnectPlayer(session.getClientAddress(), DisconnectReason.TimeOut);
            }
        }

        synchronized (nonAuthedConnections) {
            Iterator<Map.Entry<SocketAddress, ConnectionData>> connectIterator = nonAuthedConnections.entrySet().iterator();
            while (connectIterator.hasNext()) {
                Map.Entry<SocketAddress, ConnectionData> entry = connectIterator.next();
                ConnectionData connectionData = entry.getValue();
                if (currentTime - connectionData.getTimestamp() > 5_000) {
                    LOGGER.debug("Non authed session has been closed by timeout.");
                    connectIterator.remove();
                }
            }
        }
    }

    private void disconnectPlayer(SocketAddress address, DisconnectReason reason) {
        NetworkPlayerSession session = addressToSession.get(address);
        OutgoingDisconnect disconnectPacket = new OutgoingDisconnect(reason);
        incomingQueue.add(new NetworkIncomingMessage(disconnectPacket.Serialize(), NetworkIncomingMessage.NETWORK_INCOMING_DELETE_PLAYER, session.getPlayerID()));

        playerIdToSession.remove(session.getPlayerID());
        addressToSession.remove(session.getClientAddress());
        confirmedPacketsQueue.remove(session.getClientAddress());
        if (callbackSessionTimeout != null)
            callbackSessionTimeout.run();
    }

    public static class ConnectionData {
        private enum ConnectionStateMachine {
            INITIAL,
            VERSION_CHECKED,
            INVITATION_CHECKED,
            CONNECTED,
        }

        ConnectionStateMachine state;
        long lastConnectionTimestamp = 0;

        public ConnectionData() {
            this.state = ConnectionStateMachine.INITIAL;
            lastConnectionTimestamp = System.currentTimeMillis();
        }

        public ConnectionStateMachine getState() {
            return state;
        }

        public ConnectionData setState(ConnectionStateMachine state) {
            this.state = state;
            return this;
        }

        public long getTimestamp() {
            return lastConnectionTimestamp;
        }

        public ConnectionData updateTimestamp() {
            this.lastConnectionTimestamp = System.currentTimeMillis();
            return this;
        }
    }
}

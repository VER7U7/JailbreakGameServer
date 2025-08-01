package com.VER7U7.Server.Network;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.Network.Exceptions.IllegalPacketFormatException;
import com.VER7U7.Server.Network.States.NetworkIncomingMessage;
import com.VER7U7.Server.Network.States.NetworkOutgoingMessage;
import com.VER7U7.Server.Network.States.NetworkPlayerSession;
import com.VER7U7.Server.Network.States.NetworkWaitConfirmMessage;
import com.VER7U7.Server.Objects.JailPlayer;
import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.VER7U7.Server.JailConstants.*;
import static com.VER7U7.Server.Network.NetworkConstants.*;
import static com.VER7U7.Server.Packets.PacketConstants.*;
import static com.VER7U7.Server.Packets.OutgoingPacketData.*;

public class NetworkEngine extends Thread {
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

    public AtomicBoolean networkReady = new AtomicBoolean(false);
    private Runnable callbackSessionTimeout;

    public JailPools jailPools;

    public NetworkEngine(int port) {
        this.port = port;

        this.addressToSession = new ConcurrentHashMap<>();
        this.playerIdToSession = new ConcurrentHashMap<>();
        this.nonAuthedConnections = new ConcurrentHashMap<>();
        this.waitConfirmationPackets = new ConcurrentHashMap<>();

        this.incomingQueue = new ConcurrentLinkedQueue<>();
        this.outgoingQueue = new ConcurrentLinkedQueue<>();

        NetworkLog.println("UDP Server started on port " + port);
    }

    public NetworkEngine StartNetwork() throws IOException {
        this.selector = Selector.open();
        this.channel = DatagramChannel.open();
        this.channel.configureBlocking(false);
        this.channel.bind(new InetSocketAddress(port));
        this.channel.register(selector, SelectionKey.OP_READ);

        this.start();
        networkReady.set(true);
        return this;
    }

    public boolean getNetworkAvailable() {
        return networkReady.get();
    }

    public void StopNetwork() {
        this.networkReady.set(false);
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
            }catch (IOException e) {
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
        if (packet.getPacketId() == IncomingPacketType.Ask.getID()) {
            JailPlayer player = jailPools.playersPool.get(session.getPlayerID());
            if (packet.getData()[0] == 0) {
                OutgoingAsk outgoingAsk = new OutgoingAsk((byte) 1, System.currentTimeMillis());
                addPacketToOutgoing(outgoingAsk.Serialize(), 0, session.getPlayerID());
                player.askSendTime = System.currentTimeMillis();
            } else if (packet.getData()[0] == 2) {
                long deltaTime = System.currentTimeMillis() - player.askSendTime;
                player.RTT = (int)deltaTime;
                System.out.println(deltaTime);
            }
            return true;
        }
        return false;
    }

    public boolean updateReceiptConfirmation(NetworkPacket packet, SocketAddress address) {
        if (packet.getPacketId() == IncomingPacketType.ConfirmASK.getID()) {
            int countTransfers = packet.getData().length % 4;
            ByteBuffer buffer = LittleByteBuffer.wrap(packet.getData());

            for (int i = 0; i < countTransfers; i++) {
                int transferID = buffer.getInt();

                NetworkWaitConfirmMessage waitMessage = waitConfirmationPackets.get(transferID);
                if (waitMessage == null)
                    continue;

                if (waitMessage.getClientAddress() != address)
                    continue;

                waitConfirmationPackets.remove(transferID);
            }

            return true;
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
                    NetworkLog.println("Added new player");
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
                NetworkLog.errprintnln("Unknown session, may be client break connection.");
            }
        }
    }

    private void processWaitConfirmationPackets() {

        Iterator<Map.Entry<Integer, NetworkWaitConfirmMessage>> iterator = waitConfirmationPackets.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, NetworkWaitConfirmMessage> entry = iterator.next();
            long currentTime = System.currentTimeMillis();
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
                    if (playerId == -1)
                        iterator.remove();

                    int RTT = jailPools.playersPool.get(playerId).RTT + 8;
                    if (RTT > NEJB_PLAYER_TIMEOUT_MS)
                        RTT = (int)NEJB_CONFIRM_NOT_LOGIN_MS;

                    int confirmDelay = RTT * 2;

                    if ((currentTime - waitMessage.getLastTryTimestamp()) > confirmDelay) {
                        waitMessage.setLastTryTimestamp(currentTime);
                        channel.send(waitMessage.getPacket().getFormattedDataBuffer(), waitMessage.getClientAddress());
                    }
                }
            }catch(IOException e) {
                e.printStackTrace();
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
                NetworkLog.println("Session " + session.getClientAddress() + " has been closed by timeout.");
                iterator.remove();
                incomingQueue.add(new NetworkIncomingMessage(null, NetworkIncomingMessage.NETWORK_INCOMING_DELETE_PLAYER, session.getPlayerID()));
                playerIdToSession.remove(session.getPlayerID());
                if (callbackSessionTimeout != null)
                    callbackSessionTimeout.run();
            }
        }

        synchronized (nonAuthedConnections) {
            Iterator<Map.Entry<SocketAddress, ConnectionData>> connectIterator = nonAuthedConnections.entrySet().iterator();
            while (connectIterator.hasNext()) {
                Map.Entry<SocketAddress, ConnectionData> entry = connectIterator.next();
                ConnectionData connectionData = entry.getValue();
                if (currentTime - connectionData.getTimestamp() > 5_000) {
                    NetworkLog.println("Non authed session has been closed by timeout.");
                    connectIterator.remove();
                }
            }
        }
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

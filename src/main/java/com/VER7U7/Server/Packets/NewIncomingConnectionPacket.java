package com.VER7U7.Server.Packets;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.JailServer;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import java.util.Random;

import static com.VER7U7.Server.Packets.PacketConstants.*;

public class NewIncomingConnectionPacket implements PacketFactory {

    private static final short PACKET_ID = INCOMING_NEW_CONNECTION_PACKET;

    private Random rand = new Random();
    private NetworkEngine networkEngine;
    private JailPools jailPools;


    public void process(int playerID, NetworkPacket networkPacket) {
        /*if (!jailPools.newConnectionData.containsKey(session)) {
            if (networkPacket.getPacketId() == INCOMING_NEW_CONNECTION_PACKET) {
                String clientVersion = new String(networkPacket.getData(), StandardCharsets.US_ASCII);

                if (!clientVersion.equals(JailConstants.SERVER_VERSION)) {
                    NetworkPacket outPacket = new NetworkPacket(DISCONNECT_CAUSE_VERSION_NOT_MATCH.getBytes(StandardCharsets.US_ASCII), DISCONNECT_PACKET);
                    NetworkoutgoingMessage outgoingMessage = new NetworkoutgoingMessage(outPacket, 0, session);
                    networkEngine.getOutgoingMessages().add(outgoingMessage);
                    return;
                }

                jailPools.newConnectionData.put(session, new JailConnectionData().setGameVersion(clientVersion));
                NetworkPacket outPacket = new NetworkPacket(SERVER_VERSION.getBytes(StandardCharsets.US_ASCII), OUTGOING_CONNECTION_SEND_INVITATION_CODE_PACKET);
                NetworkoutgoingMessage outgoingMessage = new NetworkoutgoingMessage(outPacket, 0, session);
                networkEngine.getOutgoingMessages().add(outgoingMessage);

            } else {
                NetworkPacket outPacket = new NetworkPacket(DISCONNECT_CAUSE_TRY_AGAIN.getBytes(StandardCharsets.UTF_8), DISCONNECT_PACKET);
                NetworkoutgoingMessage outgoingMessage = new NetworkoutgoingMessage(outPacket, 0, session);
                networkEngine.getOutgoingMessages().add(outgoingMessage);
                return;

                //later will added ban list protection for DDOS
            }
        } else {
            if (networkPacket.getPacketId() == INCOMING_CONNECTION_INVITATION_CODE_PACKET) {
                String invitationCode = new String(networkPacket.getData(), StandardCharsets.US_ASCII);

                if (invitationCode.equals("JAILBREAK")) {

                    JailConnectionData connectionData = jailPools.newConnectionData.get(session).setConnectionKey(invitationCode);
                    if (connectionData.getGameVersion().equals(SERVER_VERSION)) {
                        if (jailPools.playersPool.size() < SERVER_MAX_PLAYERS) {
                            networkEngine.addSession(session);

                            int playerID = rand.nextInt(SERVER_MAX_PLAYERS);
                            Set<Map.Entry<NetworkPlayerSession, Integer>> sessionSet = jailPools.sessionToPlayerID.entrySet();
                            Iterator<Map.Entry<NetworkPlayerSession, Integer>> sessionIterator = sessionSet.iterator();
                            while (sessionIterator.hasNext()) {

                            }

                        }

                    }
                }
            }
        }*/

    }

    @Override
    public int initialize(JailServer jailServer, JUPPController physicsController, NetworkEngine networkEngine, JailPools jailPools) {
        this.networkEngine = networkEngine;
        this.jailPools = jailPools;
        return PACKET_ID;
    }
}

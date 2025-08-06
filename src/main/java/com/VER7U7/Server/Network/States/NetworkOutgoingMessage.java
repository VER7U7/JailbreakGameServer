package com.VER7U7.Server.Network.States;

import com.VER7U7.Server.Network.NetworkPacket;

import java.util.Objects;

public class NetworkOutgoingMessage extends NetworkMessage {

    public static final int NETWORK_OUTGOING_DEFAULT = 0;

    private int playerID;

    public NetworkOutgoingMessage(NetworkPacket packet, int messageType, int playerID) {
        super(packet, messageType);
        this.playerID = playerID;
    }

    public int getPlayerID() {
        return playerID;
    }

    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkOutgoingMessage)) return false;
        if (!super.equals(o)) return false;
        NetworkOutgoingMessage that = (NetworkOutgoingMessage) o;
        return playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID);
    }
}

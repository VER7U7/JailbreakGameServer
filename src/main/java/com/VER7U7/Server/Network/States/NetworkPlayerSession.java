package com.VER7U7.Server.Network.States;

import java.net.SocketAddress;
import java.util.Objects;

public class NetworkPlayerSession {

    private final SocketAddress clientAddress;
    private long lastSeenTimestamp;
    private int playerID;


    public NetworkPlayerSession(SocketAddress clientAddress, int playerID) {
        this.clientAddress = clientAddress;
        this.lastSeenTimestamp = System.currentTimeMillis();
        this.playerID = playerID;
    }

    public SocketAddress getClientAddress() {
        return clientAddress;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public void updateLastSeenTimestamp() {
        this.lastSeenTimestamp = System.currentTimeMillis();
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
        if (!(o instanceof NetworkPlayerSession)) return false;
        NetworkPlayerSession that = (NetworkPlayerSession) o;
        return lastSeenTimestamp == that.lastSeenTimestamp && playerID == that.playerID && Objects.equals(clientAddress, that.clientAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientAddress, lastSeenTimestamp, playerID);
    }
}

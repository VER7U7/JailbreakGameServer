package com.VER7U7.Server.Network.States;

import com.VER7U7.Server.Network.NetworkPacket;

import java.net.SocketAddress;
import java.util.Objects;

public class NetworkWaitConfirmMessage extends NetworkMessage{
    private SocketAddress clientAddress;
    private long lastTryTimestamp;
    private boolean hasNotAuth;

    public NetworkWaitConfirmMessage(NetworkPacket packet, SocketAddress clientAddress, long lastTryTimestamp, boolean hasNotAuth) {
        super(packet, 0);
        this.clientAddress = clientAddress;
        this.lastTryTimestamp = lastTryTimestamp;
        this.hasNotAuth = hasNotAuth;
    }
    public NetworkWaitConfirmMessage(NetworkPacket packet, SocketAddress clientAddress, long lastTryTimestamp) {
        this(packet, clientAddress, lastTryTimestamp, false);
    }


    public SocketAddress getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(SocketAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    public long getLastTryTimestamp() {
        return lastTryTimestamp;
    }

    public void setLastTryTimestamp(long lastTryTimestamp) {
        this.lastTryTimestamp = lastTryTimestamp;
    }

    public boolean isNotAuth() {
        return hasNotAuth;
    }

    public void setHasNotAuth(boolean hasNotAuth) {
        this.hasNotAuth = hasNotAuth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkWaitConfirmMessage)) return false;
        if (!super.equals(o)) return false;
        NetworkWaitConfirmMessage that = (NetworkWaitConfirmMessage) o;
        return lastTryTimestamp == that.lastTryTimestamp && Objects.equals(clientAddress, that.clientAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientAddress, lastTryTimestamp);
    }
}

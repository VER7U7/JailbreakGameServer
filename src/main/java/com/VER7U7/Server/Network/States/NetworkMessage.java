package com.VER7U7.Server.Network.States;

import com.VER7U7.Server.Network.NetworkPacket;

import java.util.Objects;

public abstract class NetworkMessage {
    //types
    private NetworkPacket packet;
    private int messageType;

    public NetworkMessage(NetworkPacket packet, int messageType) {
        this.messageType = messageType;
        this.packet = packet;
    }

    public NetworkPacket getPacket() {
        return packet;
    }

    public void setPacket(NetworkPacket packet) {
        this.packet = packet;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkMessage that = (NetworkMessage) o;
        return messageType == that.messageType && Objects.equals(packet, that.packet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packet, messageType);
    }
}

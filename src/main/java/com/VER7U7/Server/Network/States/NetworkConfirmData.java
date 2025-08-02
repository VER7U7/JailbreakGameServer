package com.VER7U7.Server.Network.States;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkConfirmData {

    private Queue<Integer> transfers = new LinkedBlockingQueue<>();
    private long firstConfirmAddTime;
    private long lastConfirmPacketTime;


    public Queue<Integer> getTransfers() {
        return transfers;
    }

    public void setTransfers(Queue<Integer> transfers) {
        this.transfers = transfers;
    }

    public long getFirstConfirmAddTime() {
        return firstConfirmAddTime;
    }

    public void setFirstConfirmAddTime(long firstConfirmAddTime) {
        this.firstConfirmAddTime = firstConfirmAddTime;
    }

    public long getLastConfirmPacketTime() {
        return lastConfirmPacketTime;
    }

    public void setLastConfirmPacketTime(long lastConfirmPacketTime) {
        this.lastConfirmPacketTime = lastConfirmPacketTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NetworkConfirmData)) return false;
        NetworkConfirmData that = (NetworkConfirmData) o;
        return firstConfirmAddTime == that.firstConfirmAddTime && lastConfirmPacketTime == that.lastConfirmPacketTime && Objects.equals(transfers, that.transfers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transfers, firstConfirmAddTime, lastConfirmPacketTime);
    }
}

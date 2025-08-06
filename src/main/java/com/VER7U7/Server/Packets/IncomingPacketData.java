package com.VER7U7.Server.Packets;

import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.VER7U7.Server.Packets.PacketConstants.*;

public abstract class IncomingPacketData {
    public IncomingPacketType type;

    public IncomingPacketData(IncomingPacketType type) {
        this.type = type;
    }

    public abstract void Deserialize(NetworkPacket packet);

    public static class IncomingNewConnection extends IncomingPacketData {
        public int clientVersion;

        public IncomingNewConnection () {
            super(IncomingPacketType.NewConnection);
        }

        @Override
        public void Deserialize(NetworkPacket packet) {
            this.clientVersion = LittleByteBuffer.wrap(packet.getData()).getInt();
        }
    }

    public static class IncomingConnectionInvitationCode extends IncomingPacketData {
        public String invitationCode;

        public IncomingConnectionInvitationCode() {
            super(IncomingPacketType.ConnectionInvitationCode);
        }

        @Override
        public void Deserialize(NetworkPacket packet) {
            this.invitationCode = new String(packet.getData(), StandardCharsets.US_ASCII);
        }
    }

    public static class IncomingDisconnect extends IncomingPacketData{
        public DisconnectReason disconnectReason;

        public IncomingDisconnect() {
            super(IncomingPacketType.Disconnect);
        }

        public IncomingDisconnect(NetworkPacket packet) {
            this();
            Deserialize(packet);
        }

        @Override
        public void Deserialize(NetworkPacket packet) {
            this.disconnectReason = DisconnectReason.fromID(LittleByteBuffer.wrap(packet.getData()).get() & 0xFF);
        }
    }

    public static class IncomingPing extends IncomingPacketData {
        public byte step;
        public long senderTimestamp;

        public IncomingPing() {
            super(IncomingPacketType.Ping);
        }

        public void Deserialize(NetworkPacket packet) {
            ByteBuffer byteBuffer = LittleByteBuffer.wrap(packet.getData());
            this.step = byteBuffer.get();
            this.senderTimestamp = byteBuffer.getLong();
        }
    }

    public static class IncomingConfirmAsk extends IncomingPacketData {
        public int[] transfers;

        public IncomingConfirmAsk() {
            super(IncomingPacketType.ConfirmASK);
        }

        @Override
        public void Deserialize(NetworkPacket packet) {
            int countTransfers = packet.getData().length / 4;
            transfers = new int[countTransfers];
            ByteBuffer buffer = LittleByteBuffer.wrap(packet.getData());
            for (int i = 0; i < countTransfers; i++) {
                transfers[i] = buffer.getInt();
            }
        }
    }
}

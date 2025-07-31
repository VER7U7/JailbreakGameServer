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

        @Override
        public void Deserialize(NetworkPacket packet) {
            this.disconnectReason = DisconnectReason.fromID(LittleByteBuffer.wrap(packet.getData()).get() & 0xFF);
        }
    }

    public static class IncomingAsk extends IncomingPacketData {
        public byte step;
        public long senderTimestamp;

        public IncomingAsk() {
            super(IncomingPacketType.Ask);
        }

        public void Deserialize(NetworkPacket packet) {
            ByteBuffer byteBuffer = LittleByteBuffer.wrap(packet.getData());
            this.step = byteBuffer.get();
            this.senderTimestamp = byteBuffer.getLong();
        }
    }
}

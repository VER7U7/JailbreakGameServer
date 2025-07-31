package com.VER7U7.Server.Packets;

import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.nio.ByteBuffer;

import static com.VER7U7.Server.Packets.PacketConstants.*;

public abstract class OutgoingPacketData {
    public OutgoingPacketType type;
    public OutgoingPacketData(OutgoingPacketType type) {
        this.type = type;
    }
    public abstract NetworkPacket Serialize();



    public static class OutgoingConnectionSendInvitationCode extends OutgoingPacketData {

        public int serverVersion;

        public OutgoingConnectionSendInvitationCode() {
            super(OutgoingPacketType.ConnectionSendInvitationCode);
        }

        public OutgoingConnectionSendInvitationCode(int serverVersion) {
            this();
            this.serverVersion = serverVersion;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(4).putInt(serverVersion);
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingConnectionSuccess extends OutgoingPacketData {

        public String successText = "SUCCESS";

        public OutgoingConnectionSuccess() {
            super(OutgoingPacketType.ConnectionSuccess);
        }

        public OutgoingConnectionSuccess(String successText) {
            this();
            this.successText = successText;
        }

        @Override
        public NetworkPacket Serialize() {
            return new NetworkPacket(successText.getBytes(), type.getID());
        }
    }

    public static class OutgoingAsk extends OutgoingPacketData {
        public byte step;
        public long timestamp;

        public OutgoingAsk() {
            super(OutgoingPacketType.Ask);
        }

        public OutgoingAsk(byte step, long timestamp) {
            this();
            this.step = step;
            this.timestamp = timestamp;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(9).put(step).putLong(timestamp);
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingDisconnect extends OutgoingPacketData {

        public DisconnectReason disconnectReason;

        public OutgoingDisconnect() {
            super(OutgoingPacketType.Disconnect);
        }

        public OutgoingDisconnect(DisconnectReason disconnectReason) {
            this();
            this.disconnectReason = disconnectReason;
        }

        @Override
        public NetworkPacket Serialize() {
            return new NetworkPacket(LittleByteBuffer.allocate(1).put((byte)disconnectReason.getID()).array(), type.getID());
        }
    }

}

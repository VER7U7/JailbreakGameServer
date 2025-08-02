package com.VER7U7.Server.Packets;

import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

        public short playerId;
        public String successText = "SUCCESS";

        public OutgoingConnectionSuccess() {
            super(OutgoingPacketType.ConnectionSuccess);
        }

        public OutgoingConnectionSuccess(short playerId, String successText) {
            this();
            this.playerId = playerId;
            this.successText = successText;
        }

        public OutgoingConnectionSuccess(int playerId) {
            this();
            this.playerId = (short) playerId;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(2 + successText.length()).putShort(playerId).put(successText.getBytes(StandardCharsets.US_ASCII));
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingPing extends OutgoingPacketData {
        public byte step;
        public long timestamp;

        public OutgoingPing() {
            super(OutgoingPacketType.Ping);
        }

        public OutgoingPing(byte step, long timestamp) {
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

    public static class OutgoingConfirmAsk extends OutgoingPacketData {

        public int[] transfers;

        public OutgoingConfirmAsk() {
            super(OutgoingPacketType.ConfirmAsk);
        }

        public OutgoingConfirmAsk(int[] transfers) {
            this();
            this.transfers = transfers;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(transfers.length * 4);
            for (int transfer : transfers) {
                buffer.putInt(transfer);
            }
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

}

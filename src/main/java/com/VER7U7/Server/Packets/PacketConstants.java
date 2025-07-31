package com.VER7U7.Server.Packets;

public class PacketConstants {


    public enum IncomingPacketType {
        NewConnection(1),
        ConnectionInvitationCode(2),

        Ask(7),

        Disconnect(255);

        private final int value;
        IncomingPacketType(int value) {
            this.value = value;
        }
        public int getID() {
            return value;
        }
        public static IncomingPacketType fromID(int value) {
            for (IncomingPacketType incoming : IncomingPacketType.values()) {
                if (incoming.value == value)
                    return incoming;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }


    public enum OutgoingPacketType {
        ConnectionSendInvitationCode(1),
        ConnectionSuccess(2),

        Ask(7),
        PlayerMove(10),
        ChatMessage(11),

        Disconnect(255);

        private final int value;
        OutgoingPacketType(int value) {
            this.value = value;
        }
        public int getID() {
            return value;
        }
        public static OutgoingPacketType fromID(int value) {
            for (OutgoingPacketType outgoing : OutgoingPacketType.values()) {
                if (outgoing.value == value)
                    return outgoing;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }

    public enum DisconnectReason {
        VersionNotMatch(1),
        TryAgain(2),
        WrongOrder(3),
        WrongCode(4);

        private final int value;
        DisconnectReason(int value) {
            this.value = value;
        }
        public int getID() {
            return value;
        }
        public static DisconnectReason fromID(int value) {
            for (DisconnectReason reason : DisconnectReason.values()) {
                if (reason.value == value)
                    return reason;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }
}

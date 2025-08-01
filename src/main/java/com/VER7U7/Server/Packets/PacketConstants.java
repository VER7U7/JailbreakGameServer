package com.VER7U7.Server.Packets;

public class PacketConstants {


    public enum IncomingPacketType {
        NewConnection(1, true),
        ConnectionInvitationCode(2, true),

        Ask(7, false), //Ping
        ConfirmASK(8, false),

        Disconnect(255, false);

        private final int value;
        private final boolean needConfirmation;

        IncomingPacketType(int value, boolean needConfirm) {
            this.value = value;
            this.needConfirmation = needConfirm;
        }
        public int getID() {
            return value;
        }
        public boolean hasNeedConfirm() {
            return needConfirmation;
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
        ConnectionSendInvitationCode(1, true),
        ConnectionSuccess(2, true),

        Ask(7, false), //Ping
        ConfirmAsk(8, false),

        PlayerMove(10, false),
        ChatMessage(11, true),

        Disconnect(255, false);

        private final int value;
        private final boolean needConfirmation;
        OutgoingPacketType(int value, boolean needConfirm) {
            this.value = value;
            this.needConfirmation = needConfirm;
        }
        public int getID() {
            return value;
        }
        public boolean hasNeedConfirm() {
            return needConfirmation;
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

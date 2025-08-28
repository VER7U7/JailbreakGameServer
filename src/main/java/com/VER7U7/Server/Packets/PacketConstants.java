package com.VER7U7.Server.Packets;

public class PacketConstants {


    public enum IncomingPacketType {
        NewConnection(1, true),
        ConnectionInvitationCode(2, true),

        Ping(7, false),
        ConfirmASK(8, false),

        LocalPlayerSync(12, false),

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

        Ping(7, false),
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
        VersionNotMatch(1, "Version not match"),
        TryAgain(2, "Try Again"),
        WrongOrder(3, "Wrong order"),
        WrongCode(4, "Wrong code"),
        TimeOut(5, "Time out"),
        ClientDisconnect(6, "Client disconnect"),
        InnerException(7, "Inner exception");

        private final int value;
        private final String text;
        DisconnectReason(int value, String text) {
            this.value = value; this.text = text;
        }
        public int getID() {
            return value;
        }
        public String getText() {
            return text + " ("+ value +")";
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

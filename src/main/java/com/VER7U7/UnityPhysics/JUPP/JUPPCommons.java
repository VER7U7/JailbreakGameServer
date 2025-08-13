package com.VER7U7.UnityPhysics.JUPP;

public class JUPPCommons {
    public static final int JUPP_PACKET_MAX_SIZE = 1024;
    public static final int JUPP_BUFFER_SIZE = 1024;

    public static final String JUPP_HEADER = "JUPP";
    public static final String JUPP_VERSION = "0.0.1";



    ///PACKETS ID
    public static final short JUPP_VERSION_CONTROL_PACKET = 1;
    public static final short JUPP_CREATE_OBJECT = 2;

    public enum JuppIncomingCommands {
        ;

        private int value;

        JuppIncomingCommands(int value) {
            this.value = value;
        }

        public int getID() {return value;}
        public JuppIncomingCommands fromID (int value) {
            for (JuppIncomingCommands incoming : JuppIncomingCommands.values()) {
                if(incoming.getID() == value)
                    return incoming;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }

    public enum JuppOutgoingCommands {
        VersionControlPacket(1),
        SyncEndTick(2);

        private int value;

        JuppOutgoingCommands(int value) {
            this.value = value;
        }

        public int getID() {return value;}
        public JuppOutgoingCommands fromID (int value) {
            for (JuppOutgoingCommands outgoing : JuppOutgoingCommands.values()) {
                if(outgoing.getID() == value)
                    return outgoing;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }

    public enum BridgeStatus
    {
        BridgeStarted,
        BridgeSearchConnection,
        BridgeConnected,
        BridgeStopped,
        BridgeError,
    }


}

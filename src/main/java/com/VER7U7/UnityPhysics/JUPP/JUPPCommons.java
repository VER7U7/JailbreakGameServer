package com.VER7U7.UnityPhysics.JUPP;

public class JUPPCommons {
    public static final int JUPP_PACKET_MAX_SIZE = 1024;
    public static final int JUPP_BUFFER_SIZE = 1024;

    public static final String JUPP_HEADER = "JUPP";



    public enum BridgeStatus
    {
        BridgeStarted,
        BridgeSearchConnection,
        BridgeConnected,
        BridgeStopped,
        BridgeError,
    }


}

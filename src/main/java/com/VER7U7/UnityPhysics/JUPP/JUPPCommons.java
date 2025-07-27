package com.VER7U7.UnityPhysics.JUPP;

public class JUPPCommons {
    public static final int JUPP_PACKET_MAX_SIZE = 1024;
    public static final int JUPP_BUFFER_SIZE = 1024;

    public static final String JUPP_HEADER = "JUPP";
    public static final String JUPP_VERSION = "0.0.1";



    ///PACKETS ID
    public static final short JUPP_VERSION_CONTROL_PACKET = 1;
    public static final short JUPP_CREATE_OBJECT = 2;


    public enum BridgeStatus
    {
        BridgeStarted,
        BridgeSearchConnection,
        BridgeConnected,
        BridgeStopped,
        BridgeError,
    }


}

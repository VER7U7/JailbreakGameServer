package com.VER7U7.Server.Packets;

public class PacketConstants {

    //INCOMING
    public static final short INCOMING_NEW_CONNECTION_PACKET = 1;
    public static final short INCOMING_CONNECTION_INVITATION_CODE_PACKET = 2;

    public static final short INCOMING_ASK = 7;

    //OUTGOING
    public static final short OUTGOING_CONNECTION_SEND_INVITATION_CODE_PACKET = 1;
    public static final short OUTGOING_CONNECTION_SUCCESS = 2;
    public static final short OUTGOING_ASK = 7;


    //DISCONNECT
    public static final short DISCONNECT_PACKET = 255;
    public static final String DISCONNECT_CAUSE_VERSION_NOT_MATCH = "VERSION_NOT_MATCH";
    public static final String DISCONNECT_CAUSE_TRY_AGAIN = "TRY_AGAIN";
    public static final String DISCONNECT_CAUSE_WRONG_ORDER = "DISCONNECT_CAUSE_WRONG_ORDER";
    public static final String DISCONNECT_CAUSE_WRONG_CODE = "DISCONNECT_CAUSE_WRONG_CODE";
}

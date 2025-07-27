package com.VER7U7.Server.Network;

import java.nio.charset.StandardCharsets;

public class NetworkConstants {

    public static final byte[] NEJB_PROTOCOL_HEADER = "NEJB".getBytes(StandardCharsets.US_ASCII);
    public static final String NEJB_PROTOCOL_VERSION = "0.0.1";
    public static final int NEJB_PROTOCOL_MAX_LENGTH = 1024;
    public static final long NEJB_PLAYER_TIMEOUT_MS = 10_000;


}

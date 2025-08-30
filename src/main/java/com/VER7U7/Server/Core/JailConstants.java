package com.VER7U7.Server.Core;

public class JailConstants {

    public static final int SERVER_VERSION = 1001; //0.0.1
    public static final int SERVER_TICK_RATE = 64;
    public static final long NS_PER_SERVER_TICK = 1_000_000_000L / SERVER_TICK_RATE;
    public static final int SERVER_MAX_PLAYERS = 40;


    public static final int SERVER_SYNC_RATE = 64;
    public static final long NS_PER_SYNC_RATE = 1_000_000_000L / SERVER_SYNC_RATE;

}

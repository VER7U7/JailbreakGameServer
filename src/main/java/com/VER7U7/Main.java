package com.VER7U7;

import com.VER7U7.Server.Core.JailServer;
import com.VER7U7.UnityPhysics.JUPP.*;

import java.io.IOException;

public class Main {

    public static JailServer jailServer;

    public static int PLAYER_NETWORK_PORT = 5000;
    public static int SERVICE_NETWORK_PORT = 8787;
    public static int JUBB_NETWORK_PORT = 6767;

    public static void main(String[] args) throws InterruptedException, JUPPExceptions.VersionNotMatch, IOException {
        jailServer = new JailServer();

        jailServer.StartSimulation();

    }
}
package com.VER7U7;

import com.VER7U7.Server.JailServer;
import com.VER7U7.UnityPhysics.JUPP.*;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static JUPPEngine physicsEngine;
    public static int PLAYER_NETWORK_PORT = 5000;
    public static int SERVICE_NETWORK_PORT = 8787;
    public static int JUBB_NETWORK_PORT = 6767;

    public static void main(String[] args) throws InterruptedException, JUPPExceptions.VersionNotMatch {
        //physicsEngine = new JUPPEngine(JUBB_NETWORK_PORT));
        //physicsEngine.Start();

        JailServer jailServer = new JailServer();
        jailServer.StartSimulation();
        jailServer.join();

        //physicsEngine.Close();
    }
}
package com.VER7U7.UnityPhysics.JUPP;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JUPPBridge {
    private short port;
    private Thread serverThread;
    private Socket socketConnection;

    public boolean bridgeRunning = true;
    public JUPPCommons.BridgeStatus bridgeStatus;


    public JUPPBridge(short port) {
        this.port = port;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                UpdateConnection();
            }
        });
        serverThread.start();

    }

    private void UpdateConnection() {
        bridgeStatus = JUPPCommons.BridgeStatus.BridgeStarted;

        while(bridgeRunning) {
            try (Socket socket = new Socket("localhost", port)) {
                System.out.println("Connected to unity!");

                byte[] data = "Hello world!".getBytes(StandardCharsets.US_ASCII);
                JUPPPacket juppPacket = new JUPPPacket(data, (short) data.length, false);
                juppPacket.packetID = 1;

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(juppPacket.getFormattedData());

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response;
                while((response = reader.readLine()) != null) {
                    System.out.println("Unity says: " + response);
                }
                break;
            } catch(IOException e) {
                System.out.println("Waiting for Unity server...");
                try {
                    Thread.sleep(5000);
                }catch (InterruptedException ignored) {}
            }
        }
        bridgeRunning = false;
    }
}

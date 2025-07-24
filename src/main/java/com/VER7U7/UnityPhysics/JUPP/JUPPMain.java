package com.VER7U7.UnityPhysics.JUPP;

import java.io.*;
import java.net.Socket;

public class JUPPMain {

    private short port;
    private Thread serverThread;
    private Socket socketConnection;

    public boolean bridgeWork = true;

    public JUPPMain(short port) {
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
        while(true) {
            try (Socket socket = new Socket("localhost", port)) {
                System.out.println("Connected to unity!");

                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);

                String command = "{\"type\":\"Teleport\",\"id\":1,\"pos\":[1,2,3]}";
                writer.println(command);

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
        bridgeWork = false;
    }
}

package com.VER7U7.UnityPhysics.JUPP;

import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class JUPPBridge {
    private final short port;
    private final AtomicBoolean bridgeRunning = new AtomicBoolean(true);

    private JUPPCommons.BridgeStatus bridgeStatus = JUPPCommons.BridgeStatus.BridgeStarted;

    private static final BlockingQueue<JUPPPacket> outgoingMessageQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<JUPPPacket> incomingMessageQueue = new LinkedBlockingQueue<>();

    private Socket socket;
    private Thread updateConnectionThread;
    private Thread readerThread;
    private Thread writerThread;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final Runnable restartCallback;



    public JUPPBridge(short port, Runnable restartCallback) {
        this.port = port;
        this.restartCallback = restartCallback;
    }

    public void startConnection() {
        updateConnectionThread = new Thread(this::updateConnectionLoop, "ConnectionManagerThread");
        updateConnectionThread.start();
    }

    public void stopConnection() {
        bridgeRunning.set(false);
        bridgeStatus = JUPPCommons.BridgeStatus.BridgeStopped;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }catch(IOException e) {
            JUPPLog.errprintnln("Error closing socket during stop: " + e.getMessage());
        }
    }

    public void sendPacket(JUPPPacket packet) throws SocketException {
        try {
            outgoingMessageQueue.put(packet);
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            JUPPLog.errprintnln("Send Packet interrupted: " + e.getMessage());
        }
    }

    ///non blocked method
    public JUPPPacket receiveAvalaiblePacket() {
        return incomingMessageQueue.poll();
    }

    public JUPPPacket receivePacket() throws InterruptedException {
        return incomingMessageQueue.take();
    }

    public int receivePoolSize() {
        return incomingMessageQueue.size();
    }

    private void updateConnectionLoop() {
        while (bridgeRunning.get()) {
            try {
                socket = new Socket("localhost", port);
                JUPPLog.println("Connected to Unity at " + socket.getRemoteSocketAddress());
                bridgeStatus = JUPPCommons.BridgeStatus.BridgeConnected;

                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                readerThread = new Thread(this::readerThread, "JUPPReaderThread");
                writerThread = new Thread(this::writerThread, "JUPPWriterThread");

                readerThread.start();
                writerThread.start();

                readerThread.join();
                writerThread.join();
            }catch (IOException ioe) {
                JUPPLog.println("Waiting for unity server... (" + ioe.getMessage() + ")");
                try {
                    Thread.sleep(5000);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    JUPPLog.println("Connection manager interrupted while waiting.");
                }
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                JUPPLog.println("Connection manager interrupted while joining threads.");
                bridgeStatus = JUPPCommons.BridgeStatus.BridgeError;
                break;
            }finally {
                closeResources();
                JUPPLog.println("Connection fully closed.");
                bridgeStatus = JUPPCommons.BridgeStatus.BridgeStarted; // Сброс статуса
                outgoingMessageQueue.clear();
                incomingMessageQueue.clear();
                restartCallback.run();
            }
        }
    }

    private void closeResources() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null && !socket.isClosed()) socket.close();
        }catch (IOException e) {
            JUPPLog.errprintnln("Error closing resources: " + e.getMessage());
        } finally {
            dis = null;
            dos = null;
            socket = null;
        }
    }

    private void readerThread() {
        byte[] headerBuffer = new byte[JUPPCommons.JUPP_HEADER.length()];
        byte[] lengthBuffer = new byte[2];

        try {
            while(bridgeRunning.get() && !socket.isClosed()) {
                try {
                    dis.readFully(headerBuffer, 0, JUPPCommons.JUPP_HEADER.length());

                    if (!new String(headerBuffer, StandardCharsets.US_ASCII).equals(JUPPCommons.JUPP_HEADER)) {
                        continue;
                    }

                    dis.readFully(lengthBuffer, 0, 2);
                    ByteBuffer bb = LittleByteBuffer.wrap(lengthBuffer).order(ByteOrder.LITTLE_ENDIAN);
                    short packetLength = bb.getShort();

                    if (packetLength == 0 || packetLength > JUPPCommons.JUPP_PACKET_MAX_SIZE) {
                        continue;
                    }

                    byte[] data = new byte[packetLength];
                    dis.readFully(data, 0, packetLength);

                    handleIncomingPacket(data);
                } catch(EOFException e) {
                    JUPPLog.println("[Client] Disconnected gracefully (EOF).");
                    break;
                } catch (SocketException se) {
                    JUPPLog.println("[Client] Socket Error: " + se.getMessage());
                    se.printStackTrace();
                    JUPPLog.println("[Client] Connection unexpectedly lost.");
                    break;
                } catch (IOException ioe) {
                    JUPPLog.println("[Client] IO Error: " + ioe.getMessage());
                    break;
                }
            }
        } finally {
            JUPPLog.println("JUPPReaderThread is stopped.");
            if (!writerThread.isInterrupted())
                writerThread.interrupt();
        }
    }

    private void writerThread () {
        try {
            while(bridgeRunning.get() && !socket.isClosed()) {
                JUPPPacket packet = outgoingMessageQueue.take();

                byte[] messageToSend = packet.getFormattedData();
                if (messageToSend == null) continue;

                try {
                    byte[] header = JUPPCommons.JUPP_HEADER.getBytes(StandardCharsets.US_ASCII);
                    short messageLength = (short)messageToSend.length;

                    dos.write(header);
                    dos.write(LittleByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(messageLength).array());
                    dos.write(messageToSend);
                    dos.flush();

                    //JUPPLog.println("Packet with id {" + packet.packetID + "} sent successfully.");
                }catch (SocketException se) {
                    JUPPLog.errprintnln("[Client] Socket Error in write loop: " + se.getMessage());
                    JUPPLog.errprintnln("[Client] Connection unexpectedly lost during write.");
                    break;
                } catch(IOException e) {
                    JUPPLog.errprintnln("[Client] IO Error in write loop: " + e.getMessage());
                    break;
                }
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            JUPPLog.println("JUPPWriterThread interrupted.");
        } finally {
            JUPPLog.println("JUPPWriterThread is stopped.");
            if (!readerThread.isInterrupted())
                readerThread.interrupt();
        }
    }

    private void handleIncomingPacket(byte[] rawData) {
        int packetTransferID = LittleByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).getInt();
        short packetId = LittleByteBuffer.wrap(rawData, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        byte[] data = new byte[rawData.length - 6];
        System.arraycopy(rawData, 6, data, 0, data.length);
        JUPPPacket packet = new JUPPPacket(data, packetId, packetTransferID);
        incomingMessageQueue.add(packet);
    }

    public JUPPCommons.BridgeStatus getBridgeStatus() {
        return bridgeStatus;
    }

    public void setBridgeStatus(JUPPCommons.BridgeStatus bridgeStatus) {
        this.bridgeStatus = bridgeStatus;
    }
}

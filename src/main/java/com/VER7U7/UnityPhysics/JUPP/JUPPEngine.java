package com.VER7U7.UnityPhysics.JUPP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketException;
import java.nio.channels.CancelledKeyException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.VER7U7.UnityPhysics.JUPP.JUPPCommons.*;

public class JUPPEngine {
    private static final Logger LOGGER = LogManager.getLogger(JUPPEngine.class);

    private short port;
    private JUPPBridge bridge;
    private Thread updateThread;
    private AtomicBoolean hasRunning = new AtomicBoolean(false);
    private Random rand;
    private Runnable externalRestartCallback;
    //private static final Map<Integer, JUPPPacket> resultWaitingPool = new LinkedHashMap<>();
    private static final ConcurrentHashMap<Integer, CompletableFuture<JUPPPacket>> resultWaitingPool = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, JUPPPacket> incomingPacketsPool = new ConcurrentHashMap<>();

    public JUPPEngine(int port) {
        this.port = (short)port;
    }

    public JUPPEngine(int port, Runnable externalRestartCallback) {
        this(port);
        this.externalRestartCallback = externalRestartCallback;
    }

    public void Start() throws JUPPExceptions.VersionNotMatch {
        //Starts unity physics engine
        rand = new Random();
        bridge = new JUPPBridge(port, this::RestartCallback);
        bridge.startConnection();

        updateThread = new Thread(this::UpdatePacketsThread, "JUPPEngineUpdateThread");
        updateThread.start();

        while (bridge.getBridgeStatus() != BridgeStatus.BridgeConnected) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        JUPPPacket versionRequest = new JUPPPacket(JUPP_VERSION.getBytes(StandardCharsets.US_ASCII), JUPP_VERSION_CONTROL_PACKET);
        JUPPPacket versionResult;
        try {
            if ((versionResult = sendWithResult(versionRequest)) != null && versionResult.packetID == JUPP_VERSION_CONTROL_PACKET)
                if (!new String(versionResult.getData(), StandardCharsets.US_ASCII).equals(JUPP_VERSION))
                    throw new JUPPExceptions.VersionNotMatch("Version of Unity not match with version of JUPP. Please install correct version");
                else {
                    LOGGER.debug("Connect successful!");
                    hasRunning.set(true);
                }
            else
                throw new JUPPExceptions.VersionNotMatch("Server received unknown result.");
        }catch (JUPPExceptions.VersionNotMatch vnm) {
            Close();
            throw vnm;
        }


    }

    public boolean hasRunning() {
        return hasRunning.get();
    }

    public void Join() {
        try {
            updateThread.join();
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void Close() {
        hasRunning.set(false);
        for (Map.Entry<Integer, CompletableFuture<JUPPPacket>> entry : resultWaitingPool.entrySet()) {
            entry.getValue().cancel(false);
        }
        resultWaitingPool.clear();
        incomingPacketsPool.clear();
        bridge.stopConnection();
        updateThread.interrupt();

        //Stop unity physics engine
    }

    private void RestartCallback() {
        for (Map.Entry<Integer, CompletableFuture<JUPPPacket>> entry : resultWaitingPool.entrySet()) {
            entry.getValue().cancel(false);
        }
        resultWaitingPool.clear();
        incomingPacketsPool.clear();

        if (externalRestartCallback != null)
            externalRestartCallback.run();
    }

    public BridgeStatus getBridgeStatus() {
        return bridge.getBridgeStatus();
    }

    public JUPPPacket sendWithResult(JUPPPacket outPacket) throws CancellationException {
        int transferID = 1;
        while(resultWaitingPool.containsKey(transferID) || incomingPacketsPool.containsKey(transferID)) {
            transferID = rand.nextInt(Integer.MAX_VALUE);
        }
        try {
            outPacket.packetTransferID = transferID;
            CompletableFuture<JUPPPacket> futurePacket = new CompletableFuture<>();
            resultWaitingPool.put(transferID, futurePacket);
            bridge.sendPacket(outPacket);

            return futurePacket.get();
        } catch(SocketException e) {
            LOGGER.error(e);
        }catch (CancellationException ce) {
            throw new CancellationException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("%t is interrupted.");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public JUPPPacket sendWithResultTimer(JUPPPacket outPacket, long millis) throws CancellationException, TimeoutException {
        int transferID = 1;
        while(resultWaitingPool.containsKey(transferID) || incomingPacketsPool.containsKey(transferID)) {
            transferID = rand.nextInt(Integer.MAX_VALUE);
        }
        try {
            outPacket.packetTransferID = transferID;
            CompletableFuture<JUPPPacket> futurePacket = new CompletableFuture<>();
            resultWaitingPool.put(transferID, futurePacket);
            bridge.sendPacket(outPacket);

            return futurePacket.get(millis, TimeUnit.MILLISECONDS);
        } catch(SocketException e) {
            LOGGER.error(e);
        }catch (CancellationException ce) {
            throw new CancellationException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("%t is interrupted.");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            LOGGER.error("Time to receive result out {}", outPacket.packetID);
            throw new TimeoutException();
        }
        return null;
    }

    public int sendNonBlock(JUPPPacket outPacket) {
        int transferID = 1;
        while(resultWaitingPool.containsKey(transferID) || incomingPacketsPool.containsKey(transferID)) {
            transferID = rand.nextInt(Integer.MAX_VALUE);
        }
        try {
            outPacket.packetTransferID = transferID;
            bridge.sendPacket(outPacket);
        } catch(SocketException e) {
            LOGGER.error(e);
        }
        return transferID;
    }

    private void UpdatePacketsThread() {
        try {
            while(!bridge.getBridgeStatus().equals(JUPPCommons.BridgeStatus.BridgeStopped)
                    && !bridge.getBridgeStatus().equals(JUPPCommons.BridgeStatus.BridgeError)) {
                if (bridge.getBridgeStatus().equals(BridgeStatus.BridgeStarted)) {
                    for (CompletableFuture<JUPPPacket> handler : resultWaitingPool.values()) {
                        handler.cancel(true);
                    }
                }


                JUPPPacket packet = bridge.receivePacket(); //wait packet
                if (packet == null)
                    continue;
                int transferId = packet.packetTransferID;
                CompletableFuture<JUPPPacket> futureResult = resultWaitingPool.remove(transferId);
                if (futureResult != null) {
                    futureResult.complete(packet);
                } else {
                    incomingPacketsPool.remove(packet.packetTransferID);
                    incomingPacketsPool.put(packet.packetTransferID, packet);
                }
            }
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

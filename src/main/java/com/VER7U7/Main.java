package com.VER7U7;

import com.VER7U7.UnityPhysics.JUPP.JUPPBridge;
import com.VER7U7.UnityPhysics.JUPP.JUPPLog;
import com.VER7U7.UnityPhysics.JUPP.JUPPMain;
import com.VER7U7.UnityPhysics.JUPP.JUPPPacket;

import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        /*JUPPMain juppMain = new JUPPMain((short) 6767);

        while(juppMain.bridgeWork) {}*/

        JUPPBridge juppBridge = new JUPPBridge((short)6767);
        juppBridge.startConnection();

        while(true) {

            JUPPPacket outPacket = new JUPPPacket("Hello world!".getBytes(StandardCharsets.US_ASCII), (short) 1, 1);
            try {
                juppBridge.sendPacket(outPacket);
            }catch(SocketException ignore){}

            JUPPPacket inPacket = juppBridge.receivePacket();
            if (inPacket != null) {
                if (inPacket.getPacketID() == 1) {
                    JUPPLog.println(new String(inPacket.getData(), StandardCharsets.US_ASCII));
                }
            }


            Thread.sleep(60);
        }
    }
}
package com.VER7U7.Server.PacketFunctions;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.JailServer;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import java.util.Random;

import static com.VER7U7.Server.Packets.PacketConstants.*;

public class NewIncomingConnectionPacket implements PacketFunction {

    private Random rand = new Random();
    private FunctionGlobalArgs globalArgs;


    public void process(int playerID, NetworkPacket networkPacket) {


    }

    @Override
    public IncomingPacketType initialize(FunctionGlobalArgs globalArgs) {
        this.globalArgs = globalArgs;
        return IncomingPacketType.NewConnection;
    }
}

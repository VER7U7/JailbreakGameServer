package com.VER7U7.Server.Packets.Handlers;

import com.VER7U7.Server.Network.NetworkPacket;

import java.util.Random;

import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;

public class NewIncomingConnectionPacket implements PacketFunction {

    private Random rand = new Random();
    private FunctionGlobalArgs globalArgs;


    public void process(short playerID, NetworkPacket networkPacket) {


    }

    @Override
    public IncomingPacketType initialize(FunctionGlobalArgs globalArgs) {
        this.globalArgs = globalArgs;
        return IncomingPacketType.NewConnection;
    }
}

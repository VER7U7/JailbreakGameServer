package com.VER7U7.Server.Packets.Handlers;

import com.VER7U7.Server.Network.NetworkPacket;

import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;

public interface PacketFunction {
    public void process(short playerID, NetworkPacket networkPacket);

    //need return id of packet
    public IncomingPacketType initialize(FunctionGlobalArgs globalArgs);
}

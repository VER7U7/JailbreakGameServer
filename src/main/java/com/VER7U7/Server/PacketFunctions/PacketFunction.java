package com.VER7U7.Server.PacketFunctions;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.JailServer;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import static com.VER7U7.Server.Packets.PacketConstants.*;

public interface PacketFunction {

    public void process(int playerID, NetworkPacket networkPacket);

    //need return id of packet
    public IncomingPacketType initialize(JailServer jailServer, JUPPController physicsController, NetworkEngine networkEngine, JailPools jailPools);
}

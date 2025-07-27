package com.VER7U7.Server.Packets;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.JailServer;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Network.States.NetworkPlayerSession;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

public interface PacketFactory {

    public void process(int playerID, NetworkPacket networkPacket);

    //need return id of packet
    public int initialize(JailServer jailServer, JUPPController physicsController, NetworkEngine networkEngine, JailPools jailPools);
}

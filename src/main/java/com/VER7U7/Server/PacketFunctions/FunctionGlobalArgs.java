package com.VER7U7.Server.PacketFunctions;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.JailServer;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

public class FunctionGlobalArgs {

    protected NetworkEngine networkEngine;
    protected JailPools jailPools;
    protected JUPPController physicsController;
    protected JailServer jailServer;

    public FunctionGlobalArgs(JailServer jailServer, JUPPController physicsController, NetworkEngine networkEngine, JailPools jailPools) {
        this.jailServer = jailServer;
        this.physicsController = physicsController;
        this.networkEngine = networkEngine;
        this.jailPools = jailPools;
    }

}

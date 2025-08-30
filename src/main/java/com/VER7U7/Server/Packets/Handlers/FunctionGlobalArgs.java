package com.VER7U7.Server.Packets.Handlers;

import com.VER7U7.Server.Core.JailPools;
import com.VER7U7.Server.Core.JailServer;
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

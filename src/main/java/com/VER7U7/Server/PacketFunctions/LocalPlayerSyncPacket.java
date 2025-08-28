package com.VER7U7.Server.PacketFunctions;

import com.VER7U7.Server.JailConstants;
import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.Network.NetworkPacket;
import static com.VER7U7.Server.Packets.IncomingPacketData.*;

import com.VER7U7.Server.Objects.JailPlayer;
import com.VER7U7.Server.Packets.PacketConstants;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import static com.VER7U7.Server.Packets.PacketConstants.*;

public class LocalPlayerSyncPacket implements PacketFunction{

    private JailPools jailPools;
    private JUPPController physicsController;

    @Override
    public void process(int playerID, NetworkPacket networkPacket) {
        IncomingLocalPlayerSync syncPacket = new IncomingLocalPlayerSync();
        syncPacket.Deserialize(networkPacket);

        JailPlayer player = jailPools.playersPool.get(playerID);

        if (player.unityInstanceID == 0)
            return;

        if (System.nanoTime() - player.nsLastLocalPlayerSyncTime <= JailConstants.NS_PER_SYNC_RATE)
            return;


        if (physicsController.playerClientSync(syncPacket, player))
            player.nsLastLocalPlayerSyncTime = System.nanoTime();
    }

    @Override
    public IncomingPacketType initialize(FunctionGlobalArgs globalArgs) {
        jailPools = globalArgs.jailPools;
        physicsController = globalArgs.physicsController;
        return IncomingPacketType.LocalPlayerSync;
    }
}

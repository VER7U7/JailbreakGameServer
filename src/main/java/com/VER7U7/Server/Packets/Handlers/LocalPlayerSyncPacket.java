package com.VER7U7.Server.Packets.Handlers;

import com.VER7U7.Server.Core.JailConstants;
import com.VER7U7.Server.Core.JailPools;
import com.VER7U7.Server.Network.NetworkPacket;
import static com.VER7U7.Server.Packets.Data.IncomingPacketData.*;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;

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

        if (player.state != JailPlayer.PlayerState.PlayerAlive)
            return;

        if (System.nanoTime() - player.nsLastLocalPlayerSyncTime <= JailConstants.NS_PER_SYNC_RATE)
            return;

        if (syncPacket.tick <= player.localPosSyncTick)
            return;

        if (physicsController.playerClientSync(syncPacket, player)) {
            player.localPosSyncTick = syncPacket.tick;
            player.nsLastLocalPlayerSyncTime = System.nanoTime();
        }
    }

    @Override
    public IncomingPacketType initialize(FunctionGlobalArgs globalArgs) {
        jailPools = globalArgs.jailPools;
        physicsController = globalArgs.physicsController;
        return IncomingPacketType.LocalPlayerSync;
    }
}

package com.VER7U7.Server.Packets.Handlers;

import com.VER7U7.Server.Core.JailConstants;
import com.VER7U7.Server.Core.JailPools;
import com.VER7U7.Server.Network.NetworkPacket;
import static com.VER7U7.Server.Packets.Data.IncomingPacketData.*;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;

public class LocalPlayerSyncFootPacket implements PacketFunction{

    private JailPools jailPools;
    private JUPPController physicsController;

    @Override
    public void process(short playerID, NetworkPacket networkPacket) {
        IncomingLocalFootInputSync syncPacket = new IncomingLocalFootInputSync();
        syncPacket.Deserialize(networkPacket);

        JailPlayer player = jailPools.playersPool.get(playerID);

        if (player == null || player.unityInstanceID == 0)
            return;

        if (player.state != JailPlayer.PlayerState.PlayerAlive)
            return;

        if (System.nanoTime() - player.nsLastLocalPlayerSyncTime <= JailConstants.NS_PER_SYNC_RATE)
            return;

        if (syncPacket.tick <= player.lastFootInputSyncTick)
            return;

        if (physicsController.inputClientSync(syncPacket, player)) {
            player.lastFootInputSyncTick = syncPacket.tick;
            player.nsLastLocalPlayerSyncTime = System.nanoTime();
        }
    }

    @Override
    public IncomingPacketType initialize(FunctionGlobalArgs globalArgs) {
        jailPools = globalArgs.jailPools;
        physicsController = globalArgs.physicsController;
        return IncomingPacketType.LocalInputSync;
    }
}

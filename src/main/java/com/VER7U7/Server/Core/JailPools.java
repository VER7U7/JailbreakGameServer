package com.VER7U7.Server.Core;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class JailPools {
    public ConcurrentMap<Integer, JailPlayer> playersPool;

    //Syncers
    public ConcurrentLinkedQueue<JailPlayer> deletedPlayersPool;
    public ConcurrentLinkedQueue<JailPlayer> addedPlayersPool;

    public JailPools InitializePools() {
        playersPool = new ConcurrentHashMap<>();
        deletedPlayersPool = new ConcurrentLinkedQueue<>();
        addedPlayersPool = new ConcurrentLinkedQueue<>();
        return this;
    }

    public void AddPlayer(JailPlayer player) {
        playersPool.put((int)player.playerID, player);
        addedPlayersPool.add(player);
    }

    public void DeletePlayer(int playerID) {
        deletedPlayersPool.add(playersPool.get(playerID));
        playersPool.remove(playerID);
    }

}

package com.VER7U7.Server.Core;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import static com.VER7U7.Server.Core.JailConstants.SERVER_MAX_PLAYERS;

public class JailPools {
    public ConcurrentMap<Short, JailPlayer> playersPool;

    //Syncers
    public ConcurrentLinkedQueue<JailPlayer> deletedPlayersPool;
    public ConcurrentLinkedQueue<JailPlayer> addedPlayersPool;

    public JailPools InitializePools() {
        playersPool = new ConcurrentHashMap<>(SERVER_MAX_PLAYERS);
        deletedPlayersPool = new ConcurrentLinkedQueue<>();
        addedPlayersPool = new ConcurrentLinkedQueue<>();
        return this;
    }

    public void AddPlayer(JailPlayer player) {
        playersPool.put(player.playerID, player);
        addedPlayersPool.add(player);
    }

    public void DeletePlayer(short playerID) {
        deletedPlayersPool.add(playersPool.get(playerID));
        playersPool.remove(playerID);
    }

}

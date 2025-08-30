package com.VER7U7.Server.Core;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JailPools {
    public ConcurrentMap<Integer, JailPlayer> playersPool;

    public JailPools InitializePools() {
        playersPool = new ConcurrentHashMap<>();
        return this;
    }

    public void DeletePlayer(int playerID) {
        playersPool.remove(playerID);
    }

}

package com.VER7U7.Server;

import com.VER7U7.Server.Network.States.NetworkPlayerSession;
import com.VER7U7.Server.Objects.JailPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JailPools {
    public ConcurrentMap<Integer, JailPlayer> playersPool;



    public JailPools InitializePools() {
        playersPool = new ConcurrentHashMap<>();
        return this;
    }

}

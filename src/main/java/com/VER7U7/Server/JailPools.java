package com.VER7U7.Server;

import com.VER7U7.Server.Network.States.NetworkPlayerSession;
import com.VER7U7.Server.Objects.JailPlayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JailPools {
    public ConcurrentMap<Integer, JailPlayer> playersPool;


    //Connection
    //List<PreConnectionMessage> add after creation main server
    public ConcurrentMap<NetworkPlayerSession, Integer> sessionToPlayerID;
    public ConcurrentMap<NetworkPlayerSession, JailConnectionData> newConnectionData;

    public JailPools InitializePools() {
        playersPool = new ConcurrentHashMap<>();
        newConnectionData = new ConcurrentHashMap<>();
        sessionToPlayerID = new ConcurrentHashMap<>();
        return this;
    }

}

package com.VER7U7.Server;

public class JailConnectionData {

    public String gameVersion;
    public String connectionKey;


    public String getGameVersion() {
        return gameVersion;
    }

    public JailConnectionData setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
        return this;
    }

    public String getConnectionKey() {
        return connectionKey;
    }

    public JailConnectionData setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
        return this;
    }
}

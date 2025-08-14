package com.VER7U7.Server.ObjectFactories;

import com.VER7U7.Server.Objects.JailPlayer;

public class JailPlayerFactory {

    private JailPlayer player = new JailPlayer();

    public static JailPlayerFactory createPlayer(int playerID) {
        return new JailPlayerFactory().setPlayerId((short)playerID);
    }

    public JailPlayer build() {
        return player;
    }

    public JailPlayerFactory setPlayerId(short playerId) {
        player.playerID = playerId;
        return this;
    }

    public JailPlayerFactory setNickName(String nickName) {
        player.nickname = nickName;
        return this;
    }

}

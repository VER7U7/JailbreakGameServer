package com.VER7U7.Server.Gameplay.EntityFactories;

import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.Server.Gameplay.Rules.BasicRules;

public class JailPlayerFactory {

    private JailPlayer player = new JailPlayer();

    public static JailPlayerFactory createPlayer(short playerID) {
        return new JailPlayerFactory().setplayerID((short)playerID);
    }

    public JailPlayer build() {
        return player;
    }

    public JailPlayerFactory setplayerID(short playerID) {
        player.playerID = playerID;
        return this;
    }

    public JailPlayerFactory setNickName(String nickName) {
        player.nickname = nickName;
        return this;
    }

    public JailPlayerFactory setPlayingTeam(BasicRules.Team team) {
        player.playingTeam = team;
        return this;
    }

}

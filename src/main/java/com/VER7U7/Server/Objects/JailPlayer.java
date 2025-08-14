package com.VER7U7.Server.Objects;

import com.VER7U7.Server.Utils.LittleByteBuffer;
import com.VER7U7.UnityPhysics.JUPP.JUPPCommons;

import java.nio.charset.StandardCharsets;

public class JailPlayer {

    //Runtime
    public short playerID;
    public int unityInstanceID;

    //Database
    public String nickname;

    //Transform
    public Vector3 position = new Vector3();

    //Network
    public int RTT;
    public long askSendTime;


    public byte[] addPlayerData() {
        byte[] nicknameBytes = nickname.getBytes(StandardCharsets.UTF_8);

        return LittleByteBuffer.allocate(2 + 2 + 2 + nicknameBytes.length)
                .putShort((short) PlayerUpdateType.AddPlayer.getID())
                .putShort(playerID)
                .putShort((short) nicknameBytes.length)
                .put(nicknameBytes)
                .array();
    }

    public byte[] deletePlayerData() {
        return LittleByteBuffer.allocate(2 + 2)
                .putShort((short) PlayerUpdateType.DeletePlayer.getID())
                .putShort(playerID)
                .array();
    }


    public enum PlayerUpdateType {
        AddPlayer(1),
        DeletePlayer(2);

        private int value;

        PlayerUpdateType(int value) {
            this.value = value;
        }

        public int getID() {return value;}
        public PlayerUpdateType fromID (int value) {
            for (PlayerUpdateType outgoing : PlayerUpdateType.values()) {
                if(outgoing.getID() == value)
                    return outgoing;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }
}

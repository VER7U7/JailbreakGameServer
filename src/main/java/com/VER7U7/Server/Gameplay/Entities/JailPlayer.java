package com.VER7U7.Server.Gameplay.Entities;

import com.VER7U7.Server.Gameplay.Rules.BasicRules;
import com.VER7U7.Server.Types.Quaternion;
import com.VER7U7.Server.Types.Vector3;
import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;

import java.nio.charset.StandardCharsets;

public class JailPlayer {

    //Runtime
    public short playerID;
    public int unityInstanceID;
    public int ticksTimeoutToSync;

    public PlayerState state;

    //Gameplay
    public BasicRules.Team playingTeam;

    //Database
    public String nickname;

    //Transform
    public int localInputSyncTick = 0;

        //player
    public Vector3 position = new Vector3();
    public Vector3 velocity = new Vector3();
    public Quaternion rotation = new Quaternion();
    public float jumpDelayTime;
    public boolean isGrounded = false;
        //camera
    public Vector3 cameraPosition = new Vector3();
    public Quaternion cameraRotation = new Quaternion();
    public float cameraOffsetZ = 0f; //its distance between player body (or camera target) and player camera.

    //timestamps
    public long nsLastLocalPlayerSyncTime;

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

    public byte[] clientInputSyncData(
            Vector3 camPos, Quaternion camRot, float offsetZ,
            float horizontal, float vertical, boolean jumpDown, boolean sprintDown) {
        return LittleByteBuffer.allocate(4 + Float.BYTES * 7 + Float.BYTES * 3 + 2)
                .putShort((short) PlayerUpdateType.ClientSyncInput.getID())
                .putShort(playerID)

                .put(camPos.getBytes())
                .put(camRot.getBytes())
                .putFloat(offsetZ)
                .putFloat(horizontal)
                .putFloat(vertical)
                .put((byte) (jumpDown ? 1 : 0))
                .put((byte) (sprintDown ? 1 : 0))
                .array();
    }

    public byte[] playerSyncData() {
        return LittleByteBuffer.allocate(4)
                .putShort((short) PlayerUpdateType.PlayerSync.getID())
                .putShort(playerID)
                .array();
    }


    public enum PlayerState {
        Spectator(2),
        PlayerAlive(3),
        PlayerDeath(4);


        private int value;

        PlayerState(int value) {
            this.value = value;
        }

        public int getID() {return value;}
        public PlayerState fromID (int value) {
            for (PlayerState outgoing : PlayerState.values()) {
                if(outgoing.getID() == value)
                    return outgoing;
            }
            throw new IllegalArgumentException("No enum constant with value " + value);
        }
    }

    public enum PlayerUpdateType {
        AddPlayer(1),
        DeletePlayer(2),
        ClientSyncInput(3),
        PlayerSync(4);

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

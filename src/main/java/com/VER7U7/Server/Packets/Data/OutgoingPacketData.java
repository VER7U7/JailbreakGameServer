package com.VER7U7.Server.Packets.Data;

import com.VER7U7.Server.Core.JailServer;
import com.VER7U7.Server.Gameplay.Entities.JailPlayer;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Types.Quaternion;
import com.VER7U7.Server.Types.Vector3;
import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;

public abstract class OutgoingPacketData {
    private static final Logger LOGGER = LogManager.getLogger(JailServer.class);

    public OutgoingPacketType type;
    public OutgoingPacketData(OutgoingPacketType type) {
        this.type = type;
    }
    public abstract NetworkPacket Serialize();



    public static class OutgoingConnectionSendInvitationCode extends OutgoingPacketData {

        public int serverVersion;

        public OutgoingConnectionSendInvitationCode() {
            super(OutgoingPacketType.ConnectionSendInvitationCode);
        }

        public OutgoingConnectionSendInvitationCode(int serverVersion) {
            this();
            this.serverVersion = serverVersion;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(4).putInt(serverVersion);
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingConnectionSuccess extends OutgoingPacketData {

        public short playerID;
        public String successText = "SUCCESS";

        public OutgoingConnectionSuccess() {
            super(OutgoingPacketType.ConnectionSuccess);
        }

        public OutgoingConnectionSuccess(short playerID, String successText) {
            this();
            this.playerID = playerID;
            this.successText = successText;
        }

        public OutgoingConnectionSuccess(short playerID) {
            this();
            this.playerID = playerID;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(2 + successText.length()).putShort(playerID).put(successText.getBytes(StandardCharsets.US_ASCII));
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingPing extends OutgoingPacketData {
        public byte step;
        public long timestamp;

        public OutgoingPing() {
            super(OutgoingPacketType.Ping);
        }

        public OutgoingPing(byte step, long timestamp) {
            this();
            this.step = step;
            this.timestamp = timestamp;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(9).put(step).putLong(timestamp);
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingDisconnect extends OutgoingPacketData {

        public DisconnectReason disconnectReason;

        public OutgoingDisconnect() {
            super(OutgoingPacketType.Disconnect);
        }

        public OutgoingDisconnect(DisconnectReason disconnectReason) {
            this();
            this.disconnectReason = disconnectReason;
        }

        @Override
        public NetworkPacket Serialize() {
            return new NetworkPacket(LittleByteBuffer.allocate(1).put((byte)disconnectReason.getID()).array(), type.getID());
        }
    }

    public static class OutgoingConfirmAsk extends OutgoingPacketData {

        public int[] transfers;

        public OutgoingConfirmAsk() {
            super(OutgoingPacketType.ConfirmAsk);
        }

        public OutgoingConfirmAsk(int[] transfers) {
            this();
            this.transfers = transfers;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(transfers.length * 4);
            for (int transfer : transfers) {
                buffer.putInt(transfer);
            }
            return new NetworkPacket(buffer.array(), type.getID());
        }
    }

    public static class OutgoingSpectatorPacket extends OutgoingPacketData {

        public int animationID;

        public OutgoingSpectatorPacket() {
            super(OutgoingPacketType.ClientSpectator);
        }

        public OutgoingSpectatorPacket(int animationID) {
            this();
            this.animationID = animationID;
        }

        @Override
        public NetworkPacket Serialize() {
            return new NetworkPacket(
                    LittleByteBuffer
                            .allocate(4)
                            .putInt(animationID)
                            .array(),
                    type.getID());
        }
    }

    public static class OutgoingSpawnPacket extends OutgoingPacketData {

        public short teamID;
        public Vector3 position = new Vector3();
        public Quaternion rotation = new Quaternion();

        public OutgoingSpawnPacket() {
            super(OutgoingPacketType.ClientSpawn);
        }

        public OutgoingSpawnPacket(short teamID, Vector3 position, Quaternion rotation) {
            this();
            this.teamID = teamID;
            this.position = position;
            this.rotation = rotation;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(2 + (Float.BYTES * 3) + (Float.BYTES * 4));
            buffer.putShort(teamID);
            buffer.put(position.getBytes());
            buffer.put(rotation.getBytes());
            return new NetworkPacket(buffer.array(), this.type.getID());
        }
    }

    public static class OutgoingDeathPacket extends OutgoingPacketData {

        public short deathReason;

        public OutgoingDeathPacket() {
            super(OutgoingPacketType.ClientDeath);
        }

        public OutgoingDeathPacket(short deathReason) {
            this();
            this.deathReason = deathReason;
        }

        @Override
        public NetworkPacket Serialize() {
            return new NetworkPacket(
                    LittleByteBuffer
                            .allocate(2)
                            .putShort(deathReason)
                            .array(),
                    this.type.getID());
        }
    }

    public static class OutgoingPlayerSyncFootPacket extends OutgoingPacketData {

        public int lastReceivedTick;
        public Vector3 playerPosition;
        public Vector3 playerVelocity;
        public Quaternion playerRotation;
        public float jumpDelayTime;
        public boolean isGrounded;

        public OutgoingPlayerSyncFootPacket() {
            super(OutgoingPacketType.PlayerSyncFoot);
        }

        public OutgoingPlayerSyncFootPacket(int lastReceivedTick, Vector3 position, Vector3 velocity, Quaternion rotation,
                                            float jumpDelayTime, boolean isGrounded) {
            this();
            this.lastReceivedTick = lastReceivedTick;
            this.playerPosition = position;
            this.playerVelocity = velocity;
            this.playerRotation = rotation;
            this.jumpDelayTime = jumpDelayTime;
            this.isGrounded = isGrounded;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(Float.BYTES * 3 + Float.BYTES * 3 + Float.BYTES * 4 + 9);
            buffer.putInt(lastReceivedTick);
            playerPosition.getBytes(buffer);
            playerVelocity.getBytes(buffer);
            playerRotation.getBytes(buffer);

            return new NetworkPacket(
                            buffer
                            .putFloat(jumpDelayTime)
                            .put((byte) (isGrounded ? 1 : 0))
                            .array(),
                    this.type.getID()
            );
        }
    }

    public static class OutgoingAddPlayerPacket extends OutgoingPacketData {

        public ConcurrentLinkedQueue<JailPlayer> addPlayersInfo;

        public OutgoingAddPlayerPacket() {
            super(OutgoingPacketType.AddPlayer);
        }

        public OutgoingAddPlayerPacket(ConcurrentLinkedQueue<JailPlayer> addPlayersInfo) {
            this();
            this.addPlayersInfo = addPlayersInfo;
        }

        @Override
        public NetworkPacket Serialize() {
            AtomicInteger nickLengthSum = new AtomicInteger();
            addPlayersInfo.forEach((value) -> nickLengthSum.addAndGet(value.nickname.getBytes(StandardCharsets.UTF_8).length));

            ByteBuffer buffer = LittleByteBuffer.allocate(
                    Short.BYTES //Count add players
                    + Short.BYTES * addPlayersInfo.size() //playerID * Count
                    + Short.BYTES * addPlayersInfo.size() //Ping * Count
                    + Short.BYTES * addPlayersInfo.size() //Team * Count
                    + Short.BYTES * addPlayersInfo.size() //State * Count
                    + Short.BYTES * addPlayersInfo.size() //Nickname length * Count
                    + Byte.BYTES * nickLengthSum.get() //Nickname * Global Length
                    + Float.BYTES * 3 * addPlayersInfo.size() //Position * Count
                    + Float.BYTES * 4 * addPlayersInfo.size() //Rotation * Count
            );

            buffer.putShort((short) addPlayersInfo.size());

            for (JailPlayer player : addPlayersInfo) {
                short playerID = player.playerID;

                buffer.putShort(playerID);
                buffer.putShort((short) player.RTT);
                buffer.putShort(player.playingTeam.getID());
                buffer.putShort(player.state.getID());
                buffer.putShort((short) player.nickname.getBytes(StandardCharsets.UTF_8).length);
                buffer.put(player.nickname.getBytes(StandardCharsets.UTF_8));
                player.position.getBytes(buffer);
                player.rotation.getBytes(buffer);
            }

            return new NetworkPacket(buffer.array(), this.type.getID());
        }
    }

    public static class OutgoingDeletePlayerPacket extends OutgoingPacketData {

        public short[] allDelete;

        public OutgoingDeletePlayerPacket() {
            super(OutgoingPacketType.DeletePlayer);
        }

        public OutgoingDeletePlayerPacket(short[] allDelete) {
            this();
            this.allDelete = allDelete;
        }

        @Override
        public NetworkPacket Serialize() {
            ByteBuffer buffer = LittleByteBuffer.allocate(
                    Short.BYTES // Count players
                    + Short.BYTES * allDelete.length//playerID
            );
            buffer.putShort((short) allDelete.length);
            for (short value : allDelete) {
                buffer.putShort(value);
            }

            return new NetworkPacket(buffer.array(), this.type.getID());
        }
    }

    public static class OutgoingAllPlayersInfoPacket extends OutgoingPacketData {
        public int tickID;
        public ConcurrentMap<Short, JailPlayer> playersInfo;

        public OutgoingAllPlayersInfoPacket() {
            super(OutgoingPacketType.AllPlayersInfo);
        }

        public OutgoingAllPlayersInfoPacket(ConcurrentMap<Short, JailPlayer> playersInfo, int tickID) {
            this();
            this.playersInfo = playersInfo;
            this.tickID = tickID;
        }

        @Override
        public NetworkPacket Serialize() {
            if (playersInfo == null)
                return null;
            if (playersInfo.isEmpty())
                return null;

            ByteBuffer buffer = LittleByteBuffer.allocate(
                    Short.BYTES //Count players
                    + Integer.BYTES // TickID
                    + Short.BYTES * playersInfo.size() //playerID * Count
                    + Short.BYTES * playersInfo.size() //RTT * Count
                    + Short.BYTES * playersInfo.size() //Player Team * Count
                    + Short.BYTES * playersInfo.size() //Player State * Count
                    + Float.BYTES * 3 * playersInfo.size() //Position * Count
                    + Float.BYTES * 3 * playersInfo.size() //Velocity * Count
                    + Float.BYTES * 4 * playersInfo.size() //Rotation * Count
                    + Float.BYTES * 3 * playersInfo.size() //Camera position * Count
                    + Float.BYTES * 4 * playersInfo.size() //Camera rotation * Count
            );
            buffer.putInt(tickID);
            buffer.putShort((short) playersInfo.size());

            for (Map.Entry<Short, JailPlayer> playerEntry : playersInfo.entrySet()) {
                short playerID = playerEntry.getValue().playerID;
                JailPlayer player = playerEntry.getValue();

                buffer.putShort(playerID);
                buffer.putShort((short) player.RTT);
                buffer.putShort(player.playingTeam.getID());
                buffer.putShort(player.state.getID());
                player.position.getBytes(buffer);
                player.velocity.getBytes(buffer);
                player.rotation.getBytes(buffer);
                player.cameraPosition.getBytes(buffer);
                player.cameraRotation.getBytes(buffer);
            }

            return new NetworkPacket(
                    buffer.array(),
                    this.type.getID()
            );
        }
    }

}

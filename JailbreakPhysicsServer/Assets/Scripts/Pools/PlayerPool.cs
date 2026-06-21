using System;
using System.Collections.Generic;
using System.Text;
using JUPP;
using UnityEngine;

public class PlayerPool : MonoBehaviour
{
    [Header("Pool Settings")] 
    public Transform playerBaseSpawnPoint;
    
    [SerializeField] private GameObject[] whiteListOfObjects;
    
    [Header("Runtime")]
    public PlayerBehaviour[] players;
    

    private static PlayerPool _instance;

    public static PlayerPool Instance()
    {
        if (_instance == null)
        {
            _instance = FindFirstObjectByType<PlayerPool>();
            if (_instance == null)
            {
                GameObject obj = new GameObject("PlayerPool");
                _instance = obj.AddComponent<PlayerPool>();
                DontDestroyOnLoad(obj);
            }
        }

        return _instance;
    }

    public void InitializePool(short poolSize)
    {
        players = new PlayerBehaviour[poolSize];
        ClearObjectChild();
    }

    private void ClearObjectChild()
    {
        List<GameObject> objectsToDestroy = new List<GameObject>();
        
        for (int i = 0; i < this.transform.childCount; i++)
        {
            GameObject child = transform.GetChild(i).gameObject;

            bool objectInWhitelist = false;
            foreach (GameObject whileObject in whiteListOfObjects)
            {
                if (child.name == whileObject.name)
                {
                    objectInWhitelist = true;
                }
            }
            
            if (!objectInWhitelist)
                objectsToDestroy.Add(child);
        }

        foreach (GameObject game in objectsToDestroy)
        {
            Destroy(game);
        }
    }

    private enum PlayerUpdateType
    {
        AddPlayer = 1,
        DeletePlayer = 2,
        ClientSyncInput = 3,
        PlayerSync = 4,
        PlayerSyncAll = 5,
    }

    public void ProcessUpdatePacket(JUPPBridge juppBridge, JUPPPacket packet)
    {
        short updateType;
        short playerId;
        
        GameObject player;
        PlayerBehaviour playerBehaviour;

        JUPPPacket resultPacket = null;
        
        JUPPUtils.SemiBinaryReader(packet.Data, (br) =>
        {
            updateType = br.ReadInt16();
            playerId = br.ReadInt16();
            
            switch ((PlayerUpdateType)updateType)
            {
                case PlayerUpdateType.AddPlayer:
                    if (players == null || players.Length == 0 || players.Length < playerId)
                    {
                        resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                        {
                            bw.Write(-1);
                        }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                        break;
                    }
                    
                    player = Instantiate(Resources.Load("Players/Player") as GameObject, transform);
                    player.transform.position = playerBaseSpawnPoint.position;
                    playerBehaviour = players[playerId] = player.GetComponent<PlayerBehaviour>();
                    
                    playerBehaviour.playerID = playerId;
                    byte[] nicknameBytes = br.ReadBytes(br.ReadInt16());
                    playerBehaviour.playerName = Encoding.UTF8.GetString(nicknameBytes);
                    playerBehaviour.transform.name = playerBehaviour.playerName;
                    
                    resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                    {
                        bw.Write(player.GetInstanceID());
                        bw.Write(JUPPUtils.Vector3ToByteArray(player.transform.position));
                    }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                    
                    
                    break;
                case PlayerUpdateType.DeletePlayer:
                    if (players[playerId] == null)
                    {
                        resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                        {
                            bw.Write(false);
                        }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                        break;
                    }
                    Destroy(players[playerId].getGameObject());
                    
                    resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                    {
                        bw.Write(true);
                    }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                    break;
                
                case PlayerUpdateType.ClientSyncInput:
                    if (players[playerId] == null)
                    {
                        resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                        {
                            bw.Write(false);
                        }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                        break;
                    }

                    player = players[playerId].getGameObject();
                    playerBehaviour = players[playerId];
                    
                    //cam
                    Vector3 cameraPosition = JUPPUtils.ByteArrayToVector3(br.ReadBytes(sizeof(float) * 3));
                    Quaternion cameraRotation = JUPPUtils.ByteArrayToQuaternion(br.ReadBytes(sizeof(float) * 4));
                    
                    float cameraOffset = br.ReadSingle();
                    //player
                    float horizontal = br.ReadSingle(), vertical = br.ReadSingle();
                    bool jumpDown = br.ReadBoolean();
                    bool sprintDown = br.ReadBoolean();

                    playerBehaviour.UpdatePlayerInput(
                        cameraPosition, cameraRotation, cameraOffset, 
                        horizontal, vertical, jumpDown, sprintDown);
                    
                    resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                    {
                        bw.Write(true);
                    }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                    break;
                
                case PlayerUpdateType.PlayerSync:
                    if (players[playerId] == null)
                    {
                        resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                        {
                            bw.Write(false);
                        }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                        break;
                    }

                    player = players[playerId].getGameObject();
                    playerBehaviour = players[playerId];

                    if (playerBehaviour.getRigidbody() == null)
                    {
                        resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                        {
                            bw.Write(false);
                        }), (int) JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                    }

                    resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                    {
                        bw.Write(true);
                        
                        //player
                        bw.Write(JUPPUtils.Vector3ToByteArray(player.transform.position));
                        bw.Write(JUPPUtils.Vector3ToByteArray(playerBehaviour.getRigidbody().linearVelocity));
                        bw.Write(JUPPUtils.QuaternionToByteArray(player.transform.rotation));
                        bw.Write(playerBehaviour.jumpDownDelayTime);
                        bw.Write(playerBehaviour.isGrounded);

                        //camera
                        bw.Write(JUPPUtils.Vector3ToByteArray(playerBehaviour.cameraPosition));
                        bw.Write(JUPPUtils.QuaternionToByteArray(playerBehaviour.cameraRotation));
                        bw.Write(playerBehaviour.offsetZ);
                    }), (int)JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                    
                    break;
                case PlayerUpdateType.PlayerSyncAll:

                    resultPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) =>
                    {
                        bw.Write(true);

                        short countOfPlayers = 0;
                        foreach (PlayerBehaviour playerEach in players)
                        {
                            if (playerEach != null && playerEach.getRigidbody() != null)
                                countOfPlayers++;
                        }
                        
                        bw.Write(countOfPlayers);

                        foreach (PlayerBehaviour playerEach in players)
                        {
                            playerBehaviour = playerEach;
                            if (playerBehaviour == null || playerBehaviour.getRigidbody() == null) continue;

                            player = players[playerEach.playerID].getGameObject();
                            
                            bw.Write(playerBehaviour.playerID);
                            
                            //player
                            bw.Write(JUPPUtils.Vector3ToByteArray(player.transform.position));
                            bw.Write(JUPPUtils.Vector3ToByteArray(playerBehaviour.getRigidbody().linearVelocity));
                            bw.Write(JUPPUtils.QuaternionToByteArray(player.transform.rotation));
                            bw.Write(playerBehaviour.jumpDownDelayTime);
                            bw.Write(playerBehaviour.isGrounded);

                            //camera
                            bw.Write(JUPPUtils.Vector3ToByteArray(playerBehaviour.cameraPosition));
                            bw.Write(JUPPUtils.QuaternionToByteArray(playerBehaviour.cameraRotation));
                            bw.Write(playerBehaviour.offsetZ);
                        }
                        
                    }), (int)JuppOutgoingCommands.UpdatePlayerResult, packet.PacketTransferID);
                    break;
                    
                    break;
            }
        });
        
        juppBridge.SendPacket(resultPacket);
    }
}

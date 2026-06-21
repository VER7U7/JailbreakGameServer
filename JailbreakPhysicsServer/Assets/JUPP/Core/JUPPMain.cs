using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using JUPP;
using UnityEngine;

public class JUPPMain : MonoBehaviour
{

    private JUPPBridge _juppBridge;
    public long ticksCount = 0;
    
    private readonly ConcurrentQueue<JUPPPacket> _incomingPacketQueue = new ConcurrentQueue<JUPPPacket>();
    
    void Awake()
    {
        _juppBridge = new JUPPBridge(6767);
        _juppBridge.StartConnection();

        Time.fixedDeltaTime = 1f / JUPPCommons.JUPP_TICK_RATE;
        Application.targetFrameRate = JUPPCommons.JUPP_TICK_RATE;
    }
    
    void OnEnable()
    {
        if (_juppBridge == null)
        {
            _juppBridge = new JUPPBridge(6767);
            _juppBridge.StartConnection();
        }//
    }

    private void OnDestroy()
    {
        _juppBridge?.StopConnection();
        _juppBridge = null;
    }
    
    void OnApplicationQuit()
    {
        _juppBridge?.StopConnection();
        _juppBridge = null;
    }

    void Update()
    {
        if (_juppBridge == null)
            return;
        //
        
        BlockingCollection<JUPPPacket> packetQueue = _juppBridge.GetQueuePackets();
        if (_juppBridge.BridgeStatus == JUPPCommons.BridgeStatus.BridgeConnected)
        {
            try
            {
                while (!_juppBridge.GetCancellationToken().IsCancellationRequested 
                   && packetQueue.TryTake(out var packet, Timeout.Infinite, _juppBridge.GetCancellationToken())) 
                {
                    if (packet.PacketID == (short)JuppIncomingCommands.VersionControlPacket)
                    {
                        JUPPPacket outPacket = new JUPPPacket(
                            Encoding.ASCII.GetBytes(JUPPCommons.SERVER_VERSION), (short)JuppOutgoingCommands.VersionControlPacket, packet.PacketTransferID);
                        _juppBridge.SendPacket(outPacket);
                    }

                    if (packet.PacketID == (short)JuppIncomingCommands.SyncEndTick)
                    {
                        ticksCount = BitConverter.ToInt64(packet.Data, 0);
                        JUPPPacket outPacket = new JUPPPacket(BitConverter.GetBytes(ticksCount), (int)JuppOutgoingCommands.SyncEndTick, (int)packet.PacketTransferID);
                        _juppBridge.SendPacket(outPacket);
                        return;
                    }

                    if (packet.PacketID == (short)JuppIncomingCommands.SetupPools)
                    {
                        short playerPoolSize = 0;
                        JUPPUtils.SemiBinaryReader(packet.Data, (br) =>
                        {
                            playerPoolSize = br.ReadInt16();
                        });
                        
                        PlayerPool.Instance().InitializePool(playerPoolSize);
                        
                        JUPPPacket outPacket = new JUPPPacket(JUPPUtils.SemiBinaryWriter((bw) => bw.Write(playerPoolSize)),
                            (int)JuppOutgoingCommands.SetupPoolsResult, packet.PacketTransferID);
                        _juppBridge.SendPacket(outPacket);
                    }

                    if (packet.PacketID == (short)JuppIncomingCommands.UpdatePlayer)
                    {
                        if (PlayerPool.Instance() != null)
                        {
                            PlayerPool.Instance().ProcessUpdatePacket(_juppBridge, packet);
                        }
                    }
                }
            }
            catch (OperationCanceledException) {}
            catch (Exception e)
            {
                Debug.LogException(e);
            }
        }
        
    }
}

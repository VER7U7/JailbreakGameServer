using System;
using System.Collections.Concurrent;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Text;
using UnityEngine;

namespace JUPP
{
    public class JUPPBridge
    {

        /*  JUPPBridge - main idea is bridge between Java and Unity by TCP custom protocol
         *  he work only in local network for protection from global space
         *  -- later maybe added global space mode
         */
        private short port;
        private bool _bridgeRunning = true;
        public JUPPCommons.BridgeStatus BridgeStatus { get; private set; }

        private static BlockingCollection<JUPPPacket> _outgoingQueue = new BlockingCollection<JUPPPacket>();
        private static BlockingCollection<JUPPPacket> _incomingQueue = new BlockingCollection<JUPPPacket>();
        
        private TcpListener _listener;
        private TcpClient _client;
        private NetworkStream _stream;
        
        private CancellationTokenSource _cts;
        
        private Thread _updateConnectionThread;
        private Thread _readerThread;
        private Thread _writerThread;
        
        public JUPPBridge(short port)
        {
            this.port = port;
        }

        void OnDisable()
        {
            
        }

        void OnApplicationQuit()
        {
            StopConnection();
        }

        public void StartConnection()
        {
            BridgeStatus = JUPPCommons.BridgeStatus.BridgeStarted;
            
            _updateConnectionThread = new Thread(UpdateConnectionLoop);
            _updateConnectionThread.Start();
        }

        public void StopConnection()
        {
            _bridgeRunning = false;

            try
            {
                CloseResources();
            }
            catch (IOException e)
            {
                JUPPLog.errprintln(e.ToString());
            }
        }

        public void SendPacket(JUPPPacket packet)
        {
            if (packet == null)
                return;
            _outgoingQueue.Add(packet);
        }
        
        
        /// <summary>
        /// Block main thread to get receive packet
        /// </summary>
        public JUPPPacket ReceivePacket()
        {
            return _incomingQueue.Take();
        }
        
        
        /// <summary>
        /// If queue dont have packets return false
        /// </summary>
        public bool TryReceivePacket(out JUPPPacket packet)
        {
            return _incomingQueue.TryTake(out packet);
        }

        public BlockingCollection<JUPPPacket> GetQueuePackets()
        {
            return _incomingQueue;
        }

        public CancellationToken GetCancellationToken()
        {
            return _cts.Token;
        }
        
        public void UpdateConnectionLoop()
        {
            _listener = new TcpListener(IPAddress.Parse("127.0.0.1"), port);
            _listener.Start();
            BridgeStatus = JUPPCommons.BridgeStatus.BridgeSearchConnection;
            
            while (_bridgeRunning)
            {
                try
                {
                    _cts = new CancellationTokenSource();
                    _outgoingQueue = new BlockingCollection<JUPPPacket>();
                    _incomingQueue = new BlockingCollection<JUPPPacket>();
                    
                    _client = _listener.AcceptTcpClient();
                    JUPPLog.println("Connected to JAVA server at " + _client.Client.RemoteEndPoint);
                    BridgeStatus = JUPPCommons.BridgeStatus.BridgeConnected;
                    
                    _stream = _client.GetStream();
                    
                    _readerThread = new Thread(() => ReaderThread(_cts.Token));
                    _writerThread = new Thread(() => WriterThread(_cts.Token));
                    _readerThread.Start();
                    _writerThread.Start();
                    
                    _readerThread.Join();
                    _writerThread.Join();
                }
                catch (SocketException sockEx)
                {
                    if (sockEx.SocketErrorCode == SocketError.Interrupted ||
                        sockEx.SocketErrorCode == SocketError.OperationAborted)
                        JUPPLog.println($"Listener operation cancelled/aborted (likely bridge stopped).");
                    else
                        JUPPLog.println($"Socket error during accept: {sockEx.SocketErrorCode} - {sockEx.Message}");
                }
                catch (ObjectDisposedException)
                {
                    JUPPLog.println($"Listener was disposed.");
                }
                catch (Exception ex)
                {
                    JUPPLog.println($"Unexpected error in main bridge loop: {ex.Message}");
                }
                finally
                {
                    if (_client != null)
                    {
                        JUPPLog.println($"Connection finally closed.");
                        _client.Close();
                    }
                    BridgeStatus = JUPPCommons.BridgeStatus.BridgeSearchConnection;
                }
            }

            CloseResources();
        }

        private void CloseResources()
        {
            try
            {
                
                if (_client != null)
                    _client.Close();
                
                if (_listener != null)
                    _listener.Stop();
            }
            catch (IOException e)
            {
                JUPPLog.errprintln("Error closing resources " + e);
            }
        }

        public void HandleIncomingPacket(byte[] rawData)
        {
            int packetTransferID = BitConverter.ToInt32(rawData, 0);
            short packetId = BitConverter.ToInt16(rawData, 4);
            byte[] packetData = new byte[rawData.Length - 6];
            Buffer.BlockCopy(rawData,  6, packetData, 0, packetData.Length);
            JUPPPacket packet = new JUPPPacket(packetData, packetId, packetTransferID);
            _incomingQueue.Add(packet);
        }

        public void ReaderThread(CancellationToken token)
        {
            byte[] headerBuffer = new byte[JUPPCommons.JUPP_HEADER.Length];
            byte[] lengthBuffer = new byte[2];

            try
            {
                while (_bridgeRunning 
                       && _client.Connected
                       && !token.IsCancellationRequested)
                {
                    try
                    {
                        _stream.ReadExactly(headerBuffer, 0, JUPPCommons.JUPP_HEADER.Length);
                        if (!Encoding.ASCII.GetString(headerBuffer, 0, headerBuffer.Length)
                                .Equals(JUPPCommons.JUPP_HEADER))
                            continue;

                        _stream.ReadExactly(lengthBuffer, 0, 2);
                        short packetLength = BitConverter.ToInt16(lengthBuffer, 0);
                        
                        if (packetLength <= 0 || packetLength > JUPPCommons.JUPP_PACKET_MAX_SIZE)
                        {
                            continue;
                        }

                        byte[] data = new byte[packetLength];
                        _stream.ReadExactly(data, 0, packetLength);
                        
                        HandleIncomingPacket(data);
                    }
                    catch (IOException e)
                    {
                        if (e.InnerException is SocketException se)
                        {
                            if (se.SocketErrorCode == SocketError.ConnectionReset ||
                                se.SocketErrorCode == SocketError.ConnectionAborted)
                                JUPPLog.println(
                                    $"Client {_client.Client.RemoteEndPoint} disconnected unexpectedly (Socket Error: {se.SocketErrorCode}).");
                            else
                                JUPPLog.println(
                                    $"Socket error reading from {_client.Client.RemoteEndPoint}: {se.SocketErrorCode} - {se.Message}");
                        }
                        else if (e.InnerException is EndOfStreamException)
                            JUPPLog.println(
                                $"{_client.Client.RemoteEndPoint} successfully disconnected.");
                        break;
                    }
                    catch (ObjectDisposedException)
                    {
                        JUPPLog.println($"Connection to {_client.Client.RemoteEndPoint} was disposed.");
                        break;
                    }
                    catch (Exception ex)
                    {
                        JUPPLog.println(
                            $"Unexpected error reading from {_client.Client.RemoteEndPoint}: {ex.Message}");
                        break;
                    }
                }
            }
            catch (OperationCanceledException) {}
            finally
            {
                JUPPLog.println("JUPPReaderThread is stopped.");
                if (!_outgoingQueue.IsAddingCompleted)
                    _outgoingQueue.CompleteAdding();
                if (!token.IsCancellationRequested)
                    _cts.Cancel();
            }
        }

        public void WriterThread(CancellationToken token)
        {
            try
            {
                while (
                    _bridgeRunning 
                    && _client.Connected
                    && !token.IsCancellationRequested
                    && _outgoingQueue.TryTake(out JUPPPacket packet, Timeout.Infinite, token))
                {
                    try
                    {
                        byte[] messageToSend = packet.GetFormattedData();
                        if (messageToSend == null) continue;
                        
                        byte[] header = Encoding.ASCII.GetBytes(JUPPCommons.JUPP_HEADER);
                        short messageLength = (short)messageToSend.Length;

                        _stream.Write(header, 0, header.Length);
                        _stream.Write(BitConverter.GetBytes(messageLength));
                        _stream.Write(messageToSend, 0, messageToSend.Length);
                        _stream.Flush();

                        //JUPPLog.println("Packet with id {" + packet.PacketID + "} sent successfully.");
                    }
                    catch (IOException e)
                    {
                        if (e.InnerException is SocketException se)
                        {
                            if (se.SocketErrorCode == SocketError.ConnectionReset ||
                                se.SocketErrorCode == SocketError.ConnectionAborted)
                                JUPPLog.println(
                                    $"Client {_client.Client.RemoteEndPoint} disconnected unexpectedly (Socket Error: {se.SocketErrorCode}).");
                            else
                                JUPPLog.println(
                                    $"Socket error reading from {_client.Client.RemoteEndPoint}: {se.SocketErrorCode} - {se.Message}");
                        }
                        else
                            JUPPLog.println(
                                $"IO Error reading from {_client.Client.RemoteEndPoint}: {e.Message}");

                        break;
                    }
                    catch (ObjectDisposedException)
                    {
                        JUPPLog.println($"Connection to {_client.Client.RemoteEndPoint} was disposed.");
                        break;
                    }
                    catch (InvalidOperationException ioe)
                    {
                        break;
                    }
                    catch (Exception ex)
                    {
                        JUPPLog.println(
                            $"Unexpected error reading from {_client.Client.RemoteEndPoint}: {ex.Message}");
                        break;
                    }
                }
            }
            catch (OperationCanceledException) {}
            finally
            {
                JUPPLog.println("JUPPWriterThread is stopped.");
                if (!_incomingQueue.IsAddingCompleted)
                    _incomingQueue.CompleteAdding();
                
                if (!token.IsCancellationRequested) 
                    _cts.Cancel();
            }
        }
        
    }
}
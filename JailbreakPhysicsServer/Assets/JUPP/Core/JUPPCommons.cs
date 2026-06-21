namespace JUPP
{
    
    public enum JuppIncomingCommands
    {
        VersionControlPacket = 1,
        SyncEndTick = 2,
        SetupPools,
        UpdatePlayer
    }

    public enum JuppOutgoingCommands
    {
        VersionControlPacket = 1,
        SyncEndTick = 2,
        SetupPoolsResult,
        UpdatePlayerResult,
    }
    
    public class JUPPCommons
    {
        public const int JUPP_PACKET_MAX_SIZE = 1024;
        public const int JUPP_BUFFER_SIZE = 1024;
        
        public const string JUPP_HEADER = "JUPP";
        public const string SERVER_VERSION = "0.0.2";

        public const short JUPP_TICK_RATE = 64;
        
        public enum BridgeStatus
        {
            BridgeStarted,
            BridgeSearchConnection,
            BridgeConnected,
            BridgeStopped,
            BridgeError,
        }
        
        
    }
}
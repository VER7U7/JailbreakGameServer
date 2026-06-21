using System;
using System.IO;
using System.Text;
using UnityEngine;

namespace JUPP
{
    public class JUPPPacket
    {
        public byte[] Data { get; }
        public short PacketID { get; set; }
        public int PacketTransferID { get; set; }


        private const int PacketIdSize = 2;
        
        public JUPPPacket(byte[] data, short packetID, int packetTransferID = 0)
        {
            this.PacketTransferID = packetTransferID;
            this.PacketID = packetID;
            Data = data;
        }

        public JUPPPacket(byte[] data, int packetID, int packetTransferID = 0) : this(data, (short)packetID,
            packetTransferID)
        {
        }


        /* use for send packets */
        public byte[] GetFormattedData() {
            if (PacketTransferID <= 0 || PacketID <= 0)
                return null;
            
            using (MemoryStream ms = new MemoryStream()) 
            {
                using (BinaryWriter bw = new BinaryWriter(ms))
                {
                    bw.Write(PacketTransferID);
                    bw.Write(PacketID);
                    bw.Write(Data);
                    return ms.ToArray();
                }
            }
        }
    }
}
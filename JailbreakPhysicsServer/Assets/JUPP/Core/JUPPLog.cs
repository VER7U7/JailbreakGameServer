using UnityEngine;

namespace JUPP
{
    public class JUPPLog
    {
        public static void println(string message)
        {
            Debug.Log("[JUPP Bridge] " + message);
        }
        
        public static void errprintln(string message)
        {
            Debug.LogError("[JUPP Bridge] " + message);
        }
    }
}
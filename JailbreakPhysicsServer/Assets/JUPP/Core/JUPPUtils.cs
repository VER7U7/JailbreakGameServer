using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;
using UnityEngine;

namespace JUPP
{
    public class JUPPUtils : MonoBehaviour
    {
        
        
        public static byte[] Vector3ToByteArray(UnityEngine.Vector3 vector)
        {
            byte[] bytes = new byte[sizeof(float) * 3];
            Buffer.BlockCopy(new float[] { vector.x, vector.y, vector.z }, 0, bytes, 0, bytes.Length);
            return bytes;
        }
        
        public static byte[] QuaternionToByteArray(UnityEngine.Quaternion quaternion)
        {
            byte[] bytes = new byte[sizeof(float) * 4];
            Buffer.BlockCopy(new float[] { quaternion.x, quaternion.y, quaternion.z, quaternion.w }, 0, bytes, 0, bytes.Length);
            return bytes;
        }

        public static UnityEngine.Vector3 ByteArrayToVector3(BinaryReader binaryReader)
        {
            return ByteArrayToVector3(binaryReader.ReadBytes(sizeof(float) * 3));
        }
        
        public static UnityEngine.Quaternion ByteArrayToQuaternion(BinaryReader binaryReader)
        {
            return ByteArrayToQuaternion(binaryReader.ReadBytes(sizeof(float) * 4));
        }

        
        public static UnityEngine.Vector3 ByteArrayToVector3(byte[] bytes)
        {
            if (bytes.Length != sizeof(float) * 3)
                throw new ArgumentException("Invalid byte array length for Vector3.");

            float[] components = new float[3];
            Buffer.BlockCopy(bytes, 0, components, 0, bytes.Length);
            return new UnityEngine.Vector3(components[0], components[1], components[2]);
        }
        
        public static UnityEngine.Quaternion ByteArrayToQuaternion(byte[] bytes)
        {
            if (bytes.Length != sizeof(float) * 4)
                throw new ArgumentException("Invalid byte array length for Vector3.");

            float[] components = new float[4];
            Buffer.BlockCopy(bytes, 0, components, 0, bytes.Length);
            return new UnityEngine.Quaternion(components[0], components[1], components[2], components[3]);
        }
        
        public static long nanoTime()
        {
            long nano = 10000L * Stopwatch.GetTimestamp();
            nano /= TimeSpan.TicksPerMillisecond;
            nano *= 100L;
            return nano;
        }
        
        public static byte[] SemiBinaryWriter(Action<BinaryWriter> write)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                using (BinaryWriter bw = new BinaryWriter(ms))
                {
                    write(bw);
                    return ms.ToArray();
                }
            }
        }

        public static void SemiBinaryReader(byte[] data, Action<BinaryReader> read)
        {
            using (MemoryStream ms = new MemoryStream(data))
            {
                using (BinaryReader br = new BinaryReader(ms))
                {
                    read(br);
                }
            }
        }
        
        public static void SemiBinaryReaderString(byte[] data, Action<BinaryReader> read)
        {
            using (MemoryStream ms = new MemoryStream(data))
            {
                using (BinaryReader br = new BinaryReader(ms, Encoding.UTF8))
                {
                    read(br);
                }
            }
        }
        
        public static bool EqualColliders(Collider a, Collider[] b)
        {
            foreach (Collider b1 in b)
                if (b1 == a)
                    return true;
            return false;
        }
        
        public static GameObject[] FindObjects(string tag)
        {
            GameObject[] objects = Resources.FindObjectsOfTypeAll<GameObject>();
            GameObject[] objectsRes = new GameObject[0];

            foreach (GameObject obj in objects)
            {
                if (obj.tag == tag)
                {
                    GameObject[] buffer = objectsRes;
                    objectsRes = new GameObject[objectsRes.Length + 1];
                    for (int i = 0; i < buffer.Length; i++)
                    {
                        objectsRes[i] = buffer[i];
                    }
                    objectsRes[buffer.Length] = obj;
                }
            }
            return objectsRes;
        }

        public static GameObject FindGameObjectByInstanceID(int instanceID)
        {
            GameObject[] objects = Resources.FindObjectsOfTypeAll<GameObject>();
            foreach (GameObject obj in objects)
            {
                if (obj.GetInstanceID() == instanceID)
                    return obj;
            }

            return null;
        }

        public static GameObject[] FindObjectsByName(string name)
        {
            GameObject[] objects = Resources.FindObjectsOfTypeAll<GameObject>();
            GameObject[] objectsRes = new GameObject[0];

            foreach (GameObject obj in objects)
            {
                if (obj.name == name)
                {
                    GameObject[] buffer = objectsRes;
                    objectsRes = new GameObject[objectsRes.Length + 1];
                    for (int i = 0; i < buffer.Length; i++)
                    {
                        objectsRes[i] = buffer[i];
                    }
                    objectsRes[buffer.Length] = obj;
                }
            }
            return objectsRes;
        }

        public static GameObject GetChildByTagName(String childTagName, GameObject whereLookFor)
        {
            for (int i = 0; i < whereLookFor.transform.childCount; i++)
            {
                if (whereLookFor.transform.GetChild(i).tag == childTagName)
                {
                    return whereLookFor.transform.GetChild(i).gameObject;
                }
            }
            return null;
        }

        public static GameObject[] GetChildsByTagName(String childTagName, GameObject whereLookFor)
        {
            List<GameObject> all_objects = new List<GameObject>();
            for (int i = 0; i < whereLookFor.transform.childCount; i++)
            {
                if (whereLookFor.transform.GetChild(i).tag == childTagName)
                {
                    all_objects.Add(whereLookFor.transform.GetChild(i).gameObject);
                }
            }
            return all_objects.ToArray();
        }

        public static GameObject GetChildByName(String childName, GameObject whereLookFor)
        {
            for (int i = 0; i < whereLookFor.transform.childCount; i++)
            {
                if (whereLookFor.transform.GetChild(i).name == childName)
                {
                    return whereLookFor.transform.GetChild(i).gameObject;
                }
            }
            return null;
        }

        public static GameObject[] GetChildsByName(String childName, GameObject whereLookFor)
        {
            List<GameObject> all_objects = new List<GameObject>();
            for (int i = 0; i < whereLookFor.transform.childCount; i++)
            {
                if (whereLookFor.transform.GetChild(i).name == childName)
                {
                    all_objects.Add(whereLookFor.transform.GetChild(i).gameObject);
                }
            }
            return all_objects.ToArray();
        }

        public static float Interpolate(float y1, float y2, float x1, float x2, float x)
        {
            float y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
            return y;
        }

        
    }

}
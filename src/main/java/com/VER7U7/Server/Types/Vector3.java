package com.VER7U7.Server.Types;

import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;

import java.nio.ByteBuffer;

public class Vector3 {
    public float x, y, z;

    public Vector3() { }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 putValues(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public byte[] getBytes() {
        return LittleByteBuffer.allocate(Float.BYTES * 3).putFloat(x).putFloat(y).putFloat(z).array();
    }

    public void fromBytes(byte[] arg) {
        ByteBuffer buffer = LittleByteBuffer.wrap(arg);
        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();
    }

    public void fromBytes(ByteBuffer buffer) {
        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();
    }
}

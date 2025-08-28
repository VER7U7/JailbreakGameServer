package com.VER7U7.Server.Objects;

import com.VER7U7.Server.Utils.LittleByteBuffer;

import java.nio.ByteBuffer;

public class Quaternion {
    public float x, y, z, w;

    public Quaternion() {}

    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public byte[] getBytes() {
        return LittleByteBuffer.allocate(Float.BYTES * 4).putFloat(x).putFloat(y).putFloat(z).putFloat(w).array();
    }

    public void fromBytes(byte[] arg) {
        ByteBuffer buffer = LittleByteBuffer.wrap(arg);
        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();
        w = buffer.getFloat();
    }

    public void fromBytes(ByteBuffer buffer) {
        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();
        w = buffer.getFloat();
    }
}

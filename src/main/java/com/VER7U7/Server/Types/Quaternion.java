package com.VER7U7.Server.Types;

import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.util.Objects;

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

    public void getBytes(ByteBuffer buffer) {
        buffer.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quaternion)) return false;
        Quaternion that = (Quaternion) o;
        return Float.compare(that.x, x) == 0 && Float.compare(that.y, y) == 0 && Float.compare(that.z, z) == 0 && Float.compare(that.w, w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }

    @Override
    public String toString() {
        return "Quaternion{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }
}

package com.VER7U7.Server.Types;

import com.VER7U7.Server.Utils.Buffers.LittleByteBuffer;

import java.nio.ByteBuffer;
import java.util.Objects;

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

    public void getBytes(ByteBuffer buffer) {
        buffer.putFloat(x).putFloat(y).putFloat(z);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector3)) return false;
        Vector3 vector3 = (Vector3) o;
        return Float.compare(vector3.x, x) == 0 && Float.compare(vector3.y, y) == 0 && Float.compare(vector3.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Vector3{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}


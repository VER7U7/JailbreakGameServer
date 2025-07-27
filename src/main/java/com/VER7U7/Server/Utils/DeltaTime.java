package com.VER7U7.Server.Utils;

public class DeltaTime {

    private long start;
    private long end;


    public void registerStart() {
        start = System.nanoTime();
    }

    public void registerEnd() {
        end = System.nanoTime();
    }

    public long getDeltaTime() {
        return (end - start);
    }

    public long getDeltaTimeMillis() {
        return (end - start) / 1_000_000;
    }

}

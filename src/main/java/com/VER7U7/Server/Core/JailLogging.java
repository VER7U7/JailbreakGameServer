package com.VER7U7.Server.Core;

public class JailLogging {
    public static void println(String message) {
        System.out.println("[Jailbreak Server] " + message);
    }

    public static void errprintnln(String message) {
        System.err.println("[Jailbreak Server Err] " + message);
    }
}

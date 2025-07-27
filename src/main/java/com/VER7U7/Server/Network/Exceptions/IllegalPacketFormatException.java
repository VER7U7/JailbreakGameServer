package com.VER7U7.Server.Network.Exceptions;

public class IllegalPacketFormatException extends Exception{

    public IllegalPacketFormatException() {
        super();
    }

    public IllegalPacketFormatException(String message) {
        super(message);
    }

    public IllegalPacketFormatException(String message, Throwable cause) {
        super(message, cause);
    }
    public IllegalPacketFormatException(Throwable cause) {
        super(cause);
    }
}

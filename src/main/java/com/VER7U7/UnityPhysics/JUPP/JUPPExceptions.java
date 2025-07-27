package com.VER7U7.UnityPhysics.JUPP;

public class JUPPExceptions {

    public static class VersionNotMatch extends Exception {
        public VersionNotMatch() {
            super();
        }

        public VersionNotMatch(String message) {
            super(message);
        }

        public VersionNotMatch(String message, Throwable cause) {
            super(message, cause);
        }
        public VersionNotMatch(Throwable cause) {
            super(cause);
        }
    }
    public static class UnknownResult extends Exception {
        public UnknownResult() {
            super();
        }

        public UnknownResult(String message) {
            super(message);
        }

        public UnknownResult(String message, Throwable cause) {
            super(message, cause);
        }
        public UnknownResult(Throwable cause) {
            super(cause);
        }
    }
}

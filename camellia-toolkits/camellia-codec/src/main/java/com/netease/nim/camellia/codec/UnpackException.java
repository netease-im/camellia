package com.netease.nim.camellia.codec;

public class UnpackException extends RuntimeException {

    public UnpackException() {
    }

    public UnpackException(String message) {
        super(message);
    }

    public UnpackException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnpackException(Throwable cause) {
        super(cause);
    }

}

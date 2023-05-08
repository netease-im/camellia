package com.netease.nim.camellia.hot.key.common.netty.codec;

public class PackException extends RuntimeException {

    public PackException() {
    }

    public PackException(String message) {
        super(message);
    }

    public PackException(String message, Throwable cause) {
        super(message, cause);
    }

    public PackException(Throwable cause) {
        super(cause);
    }

}

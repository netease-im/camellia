package com.netease.nim.camellia.cache.core.exception;


public class CamelliaCacheException extends RuntimeException {
    public CamelliaCacheException() {
    }

    public CamelliaCacheException(String message) {
        super(message);
    }

    public CamelliaCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaCacheException(Throwable cause) {
        super(cause);
    }

    public CamelliaCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

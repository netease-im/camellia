package com.netease.nim.camellia.cache.spring;

public class CamelliaCacheSerializerException extends RuntimeException {

    public CamelliaCacheSerializerException() {
    }

    public CamelliaCacheSerializerException(String message) {
        super(message);
    }

    public CamelliaCacheSerializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaCacheSerializerException(Throwable cause) {
        super(cause);
    }

    public CamelliaCacheSerializerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

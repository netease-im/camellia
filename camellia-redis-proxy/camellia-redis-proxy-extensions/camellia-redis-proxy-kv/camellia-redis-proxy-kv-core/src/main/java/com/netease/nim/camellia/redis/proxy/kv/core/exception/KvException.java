package com.netease.nim.camellia.redis.proxy.kv.core.exception;

/**
 * Created by caojiajun on 2024/4/8
 */
public class KvException extends RuntimeException {

    public KvException() {
    }

    public KvException(String message) {
        super(message);
    }

    public KvException(String message, Throwable cause) {
        super(message, cause);
    }

    public KvException(Throwable cause) {
        super(cause);
    }

    public KvException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package com.netease.nim.camellia.redis.exception;

/**
 *
 * Created by caojiajun on 2019/8/8.
 */
public class CamelliaRedisException extends RuntimeException {

    public CamelliaRedisException() {
    }

    public CamelliaRedisException(String message) {
        super(message);
    }

    public CamelliaRedisException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaRedisException(Throwable cause) {
        super(cause);
    }

    public CamelliaRedisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package com.netease.nim.camellia.redis.toolkit.id;

/**
 *
 * Created by caojiajun on 2020/4/9.
 */
public class CamelliaIDGenerateException extends RuntimeException {

    public CamelliaIDGenerateException() {
    }

    public CamelliaIDGenerateException(String message) {
        super(message);
    }

    public CamelliaIDGenerateException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaIDGenerateException(Throwable cause) {
        super(cause);
    }

    public CamelliaIDGenerateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

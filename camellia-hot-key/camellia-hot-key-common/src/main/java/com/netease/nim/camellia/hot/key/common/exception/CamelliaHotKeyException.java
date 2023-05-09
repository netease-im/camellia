package com.netease.nim.camellia.hot.key.common.exception;

/**
 * Created by caojiajun on 2023/5/9
 */
public class CamelliaHotKeyException extends RuntimeException {

    public CamelliaHotKeyException() {
    }

    public CamelliaHotKeyException(String message) {
        super(message);
    }

    public CamelliaHotKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaHotKeyException(Throwable cause) {
        super(cause);
    }

    public CamelliaHotKeyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package com.netease.nim.camellia.hbase.exception;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class CamelliaHBaseException extends RuntimeException {
    public CamelliaHBaseException() {
    }

    public CamelliaHBaseException(String message) {
        super(message);
    }

    public CamelliaHBaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaHBaseException(Throwable cause) {
        super(cause);
    }

    public CamelliaHBaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

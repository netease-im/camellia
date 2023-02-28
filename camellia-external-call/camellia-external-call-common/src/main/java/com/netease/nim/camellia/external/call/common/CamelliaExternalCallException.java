package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/24
 */
public class CamelliaExternalCallException extends RuntimeException {

    public CamelliaExternalCallException() {
        super();
    }

    public CamelliaExternalCallException(String message) {
        super(message);
    }

    public CamelliaExternalCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public CamelliaExternalCallException(Throwable cause) {
        super(cause);
    }

    protected CamelliaExternalCallException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

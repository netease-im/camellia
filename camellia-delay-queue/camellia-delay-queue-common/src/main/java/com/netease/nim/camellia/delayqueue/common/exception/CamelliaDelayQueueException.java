package com.netease.nim.camellia.delayqueue.common.exception;

/**
 * Created by caojiajun on 2022/7/11
 */
public class CamelliaDelayQueueException extends RuntimeException {

    private final CamelliaDelayMsgErrorCode errorCode;

    public CamelliaDelayQueueException(CamelliaDelayMsgErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public CamelliaDelayQueueException(CamelliaDelayMsgErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CamelliaDelayQueueException(CamelliaDelayMsgErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public CamelliaDelayQueueException(CamelliaDelayMsgErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public CamelliaDelayQueueException(CamelliaDelayMsgErrorCode errorCode, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public CamelliaDelayMsgErrorCode getErrorCode() {
        return errorCode;
    }
}

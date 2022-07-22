package com.netease.nim.camellia.delayqueue.common.exception;

/**
 * Created by caojiajun on 2022/7/12
 */
public enum CamelliaDelayMsgErrorCode {

    SUCCESS(200),
    PARAM_WRONG(400),
    NOT_EXISTS(404),
    UNKNOWN(500),
    ;
    private final int value;

    CamelliaDelayMsgErrorCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CamelliaDelayMsgErrorCode getByValue(int value) {
        for (CamelliaDelayMsgErrorCode errorCode : CamelliaDelayMsgErrorCode.values()) {
            if (errorCode.value == value) {
                return errorCode;
            }
        }
        return null;
    }
}

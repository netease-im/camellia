package com.netease.nim.camellia.mq.isolation.core.executor;

/**
 * Created by caojiajun on 2024/2/6
 */
public enum MsgHandlerResult {
    SUCCESS(1, true),
    FAILED_WITHOUT_RETRY(2, false),
    FAILED_WITH_RETRY(3, false),
    ;
    private final int value;
    private final boolean success;

    MsgHandlerResult(int value, boolean success) {
        this.value = value;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getValue() {
        return value;
    }

    public static MsgHandlerResult byValue(int value) {
        for (MsgHandlerResult result : MsgHandlerResult.values()) {
            if (result.value == value) {
                return result;
            }
        }
        return null;
    }
}

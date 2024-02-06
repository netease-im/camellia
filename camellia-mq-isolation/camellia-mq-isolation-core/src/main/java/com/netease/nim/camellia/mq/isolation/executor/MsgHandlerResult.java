package com.netease.nim.camellia.mq.isolation.executor;

/**
 * Created by caojiajun on 2024/2/6
 */
public enum MsgHandlerResult {
    SUCCESS(true),
    FAILED_WITHOUT_RETRY(false),
    FAILED_WITH_RETRY(false),
    ;
    private final boolean success;

    MsgHandlerResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}

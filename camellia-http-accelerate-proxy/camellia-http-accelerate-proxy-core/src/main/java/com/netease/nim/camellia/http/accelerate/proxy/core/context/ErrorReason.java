package com.netease.nim.camellia.http.accelerate.proxy.core.context;

/**
 * Created by caojiajun on 2023/7/10
 */
public enum ErrorReason {

    TRANSPORT_SERVER_ROUTE_FAIL(1),
    UPSTREAM_SERVER_ROUTE_FAIL(2),
    TRANSPORT_SERVER_SELECT_FAIL(3),
    UPSTREAM_SERVER_SELECT_FAIL(4),
    UPSTREAM_CONNECT_FAIL(5),
    UPSTREAM_ERROR(6),
    ;

    private final int value;

    ErrorReason(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ErrorReason getByValue(int value) {
        for (ErrorReason reason : ErrorReason.values()) {
            if (reason.value == value) {
                return reason;
            }
        }
        return null;
    }
}

package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;

/**
 * Created by caojiajun on 2023/7/10
 */
public enum TransportServerType {

    tcp(1),
    quic(2),
    ;

    private final int value;

    TransportServerType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TransportServerType getByValue(int value) {
        for (TransportServerType type : TransportServerType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}

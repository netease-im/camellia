package com.netease.nim.camellia.core.enums;

public enum IpCheckMode {
    BLACK(1),
    WHITE(2),
    UNKNOWN(0),
    ;
    private final int value;

    IpCheckMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static IpCheckMode getByValue(int value) {
        for (IpCheckMode ipCheckMode : IpCheckMode.values()) {
            if (ipCheckMode.value == value) {
                return ipCheckMode;
            }
        }
        return UNKNOWN;
    }
}
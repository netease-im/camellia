package com.netease.nim.camellia.dashboard.constant;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
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
        for (IpCheckMode mode : IpCheckMode.values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return UNKNOWN;
    }
}

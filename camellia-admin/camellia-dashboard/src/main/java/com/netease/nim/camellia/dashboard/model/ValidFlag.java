package com.netease.nim.camellia.dashboard.model;

/**
 *
 * Created by caojiajun on 2019/5/29.
 */
public enum ValidFlag {

    VALID(1),
    NOT_VALID(0),
    ;

    private final int value;

    ValidFlag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ValidFlag getByValue(int value) {
        for (ValidFlag validFlag : ValidFlag.values()) {
            if (validFlag.getValue() == value) {
                return validFlag;
            }
        }
        return null;
    }
}

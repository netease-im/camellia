package com.netease.nim.camellia.config.model;

/**
 * Created by caojiajun on 2023/3/15
 */
public enum ConfigHistoryType {

    NAMESPACE(1),
    CONFIG(2),
    ;

    private final int value;
    ConfigHistoryType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ConfigHistoryType getByValue(int value) {
        for (ConfigHistoryType configHistoryType : ConfigHistoryType.values()) {
            if (configHistoryType.value == value) {
                return configHistoryType;
            }
        }
        return null;
    }
}

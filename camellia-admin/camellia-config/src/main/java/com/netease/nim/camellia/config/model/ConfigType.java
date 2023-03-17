package com.netease.nim.camellia.config.model;

/**
 * Created by caojiajun on 2023/3/14
 */
public enum ConfigType {
    STRING(1),//字符串
    NUMBER(2),//整数
    FLOAT_NUMBER(3),//浮点数
    BOOLEAN(4),//布尔类型
    JSON_STRING(5),//json字符串
    ;

    private final int value;
    ConfigType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ConfigType getByValue(int value) {
        for (ConfigType configType : ConfigType.values()) {
            if (configType.value == value) {
                return configType;
            }
        }
        return null;
    }
}

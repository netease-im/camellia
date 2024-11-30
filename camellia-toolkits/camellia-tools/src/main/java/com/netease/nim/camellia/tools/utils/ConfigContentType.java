package com.netease.nim.camellia.tools.utils;

/**
 * Created by caojiajun on 2023/8/2
 */
public enum ConfigContentType {
    properties,
    json,
    ;

    public static ConfigContentType getByValue(String value, ConfigContentType defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        for (ConfigContentType type : ConfigContentType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return defaultValue;
    }
}

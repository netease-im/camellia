package com.netease.nim.camellia.redis.proxy.conf;

/**
 * Created by caojiajun on 2023/8/2
 */
public enum ConfigContentType {
    properties,
    json,
    ;

    public static ConfigContentType getByValue(String value) {
        if (value == null) {
            return properties;
        }
        for (ConfigContentType type : ConfigContentType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return properties;
    }
}

package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/6
 */
public enum NamespaceType {
    MONITOR(1),
    CACHE(2),
    ;
    private final int value;

    NamespaceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static NamespaceType getByValue(int value) {
        for (NamespaceType namespaceType : NamespaceType.values()) {
            if (namespaceType.value == value) {
                return namespaceType;
            }
        }
        return null;
    }
}

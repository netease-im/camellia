package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * Created by caojiajun on 2022/11/4
 */
public enum CamelliaMergeTaskType {
    STANDALONE(1),
    CLUSTER(2),
    ;

    private final int value;
    CamelliaMergeTaskType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CamelliaMergeTaskType getByValue(int value) {
        for (CamelliaMergeTaskType type : CamelliaMergeTaskType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}

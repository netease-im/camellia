package com.netease.nim.camellia.hot.key.sdk.collect;

/**
 * Created by caojiajun on 2023/7/3
 */
public enum CollectorType {
    Caffeine(1),//lfu效果好，性能开销相比后两者更大
    ConcurrentLinkedHashMap(2),//性能好，且有lru
    ConcurrentHashMap(3),//性能好，但是满了就直接丢弃了
    ;

    private final int value;

    CollectorType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CollectorType getByValue(int value) {
        for (CollectorType collectorType : CollectorType.values()) {
            if (collectorType.value == value) {
                return collectorType;
            }
        }
        return null;
    }
}

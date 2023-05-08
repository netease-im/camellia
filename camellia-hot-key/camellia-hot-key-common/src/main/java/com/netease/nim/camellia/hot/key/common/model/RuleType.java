package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/6
 */
public enum RuleType {

    EXACT_MATCH(1),//精准匹配
    PREFIX_MATCH(2),//前缀匹配
    MATCH_ALL(3),//匹配所有

    ;

    private final int value;

    RuleType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RuleType getByValue(int value) {
        for (RuleType ruleType : RuleType.values()) {
            if (ruleType.value == value) {
                return ruleType;
            }
        }
        return null;
    }
}

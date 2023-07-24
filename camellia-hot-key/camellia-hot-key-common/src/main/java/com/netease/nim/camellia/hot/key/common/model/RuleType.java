package com.netease.nim.camellia.hot.key.common.model;

/**
 * Created by caojiajun on 2023/5/6
 */
public enum RuleType {

    exact_match(1),//精准匹配
    prefix_match(2),//前缀匹配
    match_all(3),//匹配所有
    contains(4),//包含子串
    suffix_match(5),//后缀匹配
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

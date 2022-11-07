package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * Created by caojiajun on 2022/11/7
 */
public class CamelliaMergeTaskIntKey implements CamelliaMergeTaskKey {

    private final int value;

    public CamelliaMergeTaskIntKey(int value) {
        this.value = value;
    }

    @Override
    public String serialize() {
        return String.valueOf(value);
    }
}

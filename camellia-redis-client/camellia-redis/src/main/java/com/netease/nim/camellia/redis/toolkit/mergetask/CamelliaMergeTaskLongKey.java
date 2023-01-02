package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * Created by caojiajun on 2022/11/7
 */
public class CamelliaMergeTaskLongKey implements CamelliaMergeTaskKey {

    private final long value;

    public CamelliaMergeTaskLongKey(long value) {
        this.value = value;
    }

    @Override
    public String serialize() {
        return String.valueOf(value);
    }
}

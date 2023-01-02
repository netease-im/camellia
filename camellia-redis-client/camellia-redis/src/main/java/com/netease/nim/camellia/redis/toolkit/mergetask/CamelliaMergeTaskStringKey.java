package com.netease.nim.camellia.redis.toolkit.mergetask;


/**
 * Created by caojiajun on 2022/11/4
 */
public class CamelliaMergeTaskStringKey implements CamelliaMergeTaskKey {

    private final String key;

    public CamelliaMergeTaskStringKey(String key) {
        this.key = key;
    }

    @Override
    public String serialize() {
        return key;
    }
}

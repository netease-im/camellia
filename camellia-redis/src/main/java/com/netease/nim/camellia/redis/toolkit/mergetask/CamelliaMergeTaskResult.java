package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * Created by caojiajun on 2022/11/4
 */
public class CamelliaMergeTaskResult<V> {
    public static enum Type {
        EXEC,
        LOCAL_CACHE_HIT,
        REDIS_CACHE_HIT,
        ;
    }

    private final Type type;
    private final V result;

    public CamelliaMergeTaskResult(Type type, V result) {
        this.type = type;
        this.result = result;
    }

    public Type getType() {
        return type;
    }

    public V getResult() {
        return result;
    }
}

package com.netease.nim.camellia.redis.toolkit.mergetask;

/**
 * Created by caojiajun on 2022/11/4
 */
public interface CamelliaMergeTaskResultSerializer<V> {

    byte[] serialize(V result);

    V deserialize(byte[] data);
}

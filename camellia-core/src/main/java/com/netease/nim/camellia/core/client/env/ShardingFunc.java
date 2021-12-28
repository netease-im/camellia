package com.netease.nim.camellia.core.client.env;

/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public interface ShardingFunc {

    /**
     * should return positive int
     */
    int shardingCode(byte[]... data);

}

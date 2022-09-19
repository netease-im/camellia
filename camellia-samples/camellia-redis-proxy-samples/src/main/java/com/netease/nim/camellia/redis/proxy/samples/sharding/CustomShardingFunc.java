package com.netease.nim.camellia.redis.proxy.samples.sharding;

import com.netease.nim.camellia.core.client.env.AbstractSimpleShardingFunc;

/**
 *
 * Created by caojiajun on 2020/9/27
 */
public class CustomShardingFunc extends AbstractSimpleShardingFunc {

    @Override
    public int shardingCode(byte[] key) {
        if (key == null) return 0;
        if (key.length == 0) return 0;
        int h = 0;
        for (byte d : key) {
            h = 31 * h + d;
        }
        return (h < 0) ? -h : h;
    }
}

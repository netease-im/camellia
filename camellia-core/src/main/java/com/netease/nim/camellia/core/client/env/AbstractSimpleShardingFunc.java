package com.netease.nim.camellia.core.client.env;

/**
 *
 * Created by caojiajun on 2020/9/27
 */
public abstract class AbstractSimpleShardingFunc implements ShardingFunc {

    @Override
    public final int shardingCode(byte[]... data) {
        if (data.length == 0) return 0;
        if (data.length == 1) return shardingCode(data[0]);
        int len = 0;
        for (byte[] datum : data) {
            len += datum.length;
        }
        byte[] key = new byte[len];
        int destPos = 0;
        for (byte[] datum : data) {
            System.arraycopy(datum, 0, key, destPos, datum.length);
            destPos += datum.length;
        }
        return shardingCode(key);
    }

    public abstract int shardingCode(byte[] key);
}

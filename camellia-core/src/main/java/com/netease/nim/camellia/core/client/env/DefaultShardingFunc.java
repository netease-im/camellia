package com.netease.nim.camellia.core.client.env;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class DefaultShardingFunc implements ShardingFunc {

    @Override
    public int shardingCode(byte[]... data) {
        if (data == null) return 0;
        if (data.length == 0) return 0;
        int h = 0;
        for (byte[] d : data) {
            for (byte b : d) {
                h = 31 * h + b;
            }
        }
        return (h < 0) ? -h : h;
    }
}

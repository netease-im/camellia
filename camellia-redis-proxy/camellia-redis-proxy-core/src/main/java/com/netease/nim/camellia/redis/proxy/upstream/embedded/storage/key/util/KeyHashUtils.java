package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.util;

import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyHashUtils {

    public static int hash(byte[] key) {
        return RedisClusterCRC16Utils.getCRC16(key);
    }
}

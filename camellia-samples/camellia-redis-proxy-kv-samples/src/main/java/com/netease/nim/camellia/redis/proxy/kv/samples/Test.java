package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;

/**
 * Created by caojiajun on 2024/4/28
 */
public class Test {

    public static void main(String[] args) {
        KeyMeta keyMeta = new KeyMeta(EncodeVersion.version_0, KeyType.hash, System.currentTimeMillis(), System.currentTimeMillis() + 10*1000L, BytesUtils.toBytes(1));
        byte[] bytes = keyMeta.toBytes();


        KeyMeta keyMeta1 = KeyMeta.fromBytes(bytes);

        System.out.println(keyMeta1);
    }
}

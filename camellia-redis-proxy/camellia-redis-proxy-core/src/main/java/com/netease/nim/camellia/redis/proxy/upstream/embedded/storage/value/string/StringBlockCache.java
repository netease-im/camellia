package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.string;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;

/**
 * Created by caojiajun on 2025/1/6
 */
public class StringBlockCache {

    public byte[] get(short slot, KeyInfo keyInfo) {
        return null;
    }

    public void updateBlockCache(short slot, long fileId, long offset, byte[] block) {

    }
}

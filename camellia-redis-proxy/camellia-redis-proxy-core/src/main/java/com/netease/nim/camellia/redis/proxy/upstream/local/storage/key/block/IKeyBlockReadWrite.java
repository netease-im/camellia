package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheKey;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.IBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

import java.io.IOException;


/**
 * Created by caojiajun on 2025/1/9
 */
public interface IKeyBlockReadWrite extends IBlockReadWrite {

    /**
     * 获取一个key
     * @param slot slot
     * @param key key
     * @return key
     * @throws IOException exception
     */
    KeyInfo get(short slot, CacheKey key) throws IOException;

    /**
     * 获取一个key
     * @param slot slot
     * @param key key
     * @return key
     * @throws IOException exception
     */
    KeyInfo getForCompact(short slot, CacheKey key) throws IOException;

    /**
     * get block
     * @param fileId fileId
     * @param offset offset
     * @return block
     */
    byte[] getBlock(long fileId, long offset) throws IOException;
}

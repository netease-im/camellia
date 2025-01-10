package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
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
     * @param cacheType cache type
     * @return key
     * @throws IOException exception
     */
    KeyInfo get(short slot, Key key, CacheType cacheType) throws IOException;

    /**
     * get block
     * @param fileId fileId
     * @param offset offset
     * @return block
     */
    byte[] getBlock(long fileId, long offset) throws IOException;
}

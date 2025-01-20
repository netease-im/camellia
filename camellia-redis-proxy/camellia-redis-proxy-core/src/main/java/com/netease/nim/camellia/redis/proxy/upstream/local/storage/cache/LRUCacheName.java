package com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache;

import com.netease.nim.camellia.tools.sys.MemoryInfoCollector;

/**
 * Created by caojiajun on 2025/1/20
 */
public enum LRUCacheName {

    key_cache("local.storage.key.cache.capacity"),
    key_read_block_cache("local.storage.key.block.read.cache.capacity"),
    key_write_block_cache("local.storage.key.block.write.cache.capacity"),
    string_read_cache("local.storage.string.read.cache.capacity"),
    string_write_cache("local.storage.string.write.cache.capacity"),
    string_read_block_cache("local.storage.string.block.read.cache.capacity"),
    string_write_block_cache("local.storage.string.block.write.cache.capacity"),
    ;

    private final String configKey;

    LRUCacheName(String configKey) {
        this.configKey = configKey;
    }

    public long getCapacity(long defaultSize) {
        if (defaultSize < 0) {
            long heapMemoryMax = MemoryInfoCollector.getMemoryInfo().getHeapMemoryMax();
            long size = heapMemoryMax * 3 / 5 / LRUCacheName.values().length;
            if (size <= 0) {
                size = 32 * 1024 * 1024L;
            }
            defaultSize = size;
        }
        return CacheCapacityConfigParser.parse(configKey, CacheCapacityConfigParser.toString(defaultSize));
    }
}

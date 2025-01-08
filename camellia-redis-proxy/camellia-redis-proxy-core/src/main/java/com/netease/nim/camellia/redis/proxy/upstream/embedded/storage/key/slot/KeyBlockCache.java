package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.slot;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache.CacheKey;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache.LRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.cache.SizeCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec.KeyCodec;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.util.KeyHashUtils;

import java.io.IOException;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants.EmbeddedStorageConstants.*;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyBlockCache {

    private static final String READ_CACHE_CONFIG_KEY = "embedded.storage.key.block.read.cache.capacity";
    private static final String WRITE_CACHE_CONFIG_KEY = "embedded.storage.key.block.write.cache.capacity";

    private final KeyManifest keySlotMap;
    private final FileReadWrite fileReadWrite;

    private final LRUCache<String, byte[]> readCache;

    private final LRUCache<String, byte[]> writeCache;

    public KeyBlockCache(KeyManifest keySlotMap, FileReadWrite fileReadWrite) {
        this.keySlotMap = keySlotMap;
        this.fileReadWrite = fileReadWrite;
        this.readCache = new LRUCache<>("key-read-block-cache", READ_CACHE_CONFIG_KEY, "32M", _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        this.writeCache = new LRUCache<>("key-write-block-cache", WRITE_CACHE_CONFIG_KEY, "32M", _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
    }

    /**
     * 获取一个key
     * @param slot slot
     * @param key key
     * @return key
     * @throws IOException exception
     */
    public KeyInfo get(short slot, CacheKey key) throws IOException {
        SlotInfo slotInfo = keySlotMap.get(slot);
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        int capacity = slotInfo.capacity();
        int bucketSize = capacity / _4k;
        int bucket = KeyHashUtils.hash(key.key()) % bucketSize;

        String cacheKey = slot + "|" + fileId + "|" + (offset + _4k * bucket);
        byte[] blockCache = readCache.get(cacheKey);
        if (blockCache == null) {
            blockCache = writeCache.get(cacheKey);
            if (blockCache != null) {
                readCache.put(cacheKey, blockCache);
                writeCache.delete(cacheKey);
            }
        }
        if (blockCache == null) {
            blockCache = fileReadWrite.read(fileId, offset, bucket * _4k);
            readCache.put(cacheKey, blockCache);
        }
        Map<CacheKey, KeyInfo> map = KeyCodec.decodeBucket(blockCache);
        KeyInfo data = map.get(key);
        if (data == null) {
            return KeyInfo.DELETE;
        }
        return data;
    }

    /**
     * 更新block-cache
     * @param slot slot
     * @param fileId fileId
     * @param offset offset
     * @param block block
     */
    public void updateBlockCache(short slot, long fileId, long offset, byte[] block) {
        String cacheKey = slot + "|" + fileId + "|" + offset;
        byte[] blockCache = readCache.get(cacheKey);
        if (blockCache != null) {
            readCache.put(cacheKey, block);
        } else {
            writeCache.put(cacheKey, block);
        }
    }
}

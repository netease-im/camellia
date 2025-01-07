package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.slot;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec.KeyCodec;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.util.KeyHashUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.io.IOException;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants.EmbeddedStorageConstants.*;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyBlockCache {

    private final KeyManifest keySlotMap;
    private final FileReadWrite fileReadWrite;

    private final ConcurrentLinkedHashMap<String, byte[]> readCache;

    private final ConcurrentLinkedHashMap<String, byte[]> writeCache;

    public KeyBlockCache(KeyManifest keySlotMap, FileReadWrite fileReadWrite) {
        this.keySlotMap = keySlotMap;
        this.fileReadWrite = fileReadWrite;
        readCache = new ConcurrentLinkedHashMap.Builder<String, byte[]>()
                .initialCapacity(10000)
                .maximumWeightedCapacity(10000)
                .build();
        writeCache = new ConcurrentLinkedHashMap.Builder<String, byte[]>()
                .initialCapacity(10000)
                .maximumWeightedCapacity(10000)
                .build();
    }

    /**
     * 获取一个key
     * @param slot slot
     * @param key key
     * @return key
     * @throws IOException exception
     */
    public KeyInfo get(short slot, BytesKey key) throws IOException {
        SlotInfo slotInfo = keySlotMap.get(slot);
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        int capacity = slotInfo.capacity();
        int bucketSize = capacity / _4k;
        int bucket = KeyHashUtils.hash(key.getKey()) % bucketSize;

        String cacheKey = slot + "|" + fileId + "|" + (offset + _4k * bucket);
        byte[] blockCache = readCache.get(cacheKey);
        if (blockCache == null) {
            blockCache = writeCache.get(cacheKey);
            if (blockCache != null) {
                readCache.put(cacheKey, blockCache);
                writeCache.remove(cacheKey);
            }
        }
        if (blockCache == null) {
            blockCache = fileReadWrite.read(fileId, offset, bucket * _4k);
            readCache.put(cacheKey, blockCache);
        }
        Map<BytesKey, KeyInfo> map = KeyCodec.decodeBucket(blockCache);
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

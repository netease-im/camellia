package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCacheName;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileNames;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.SizeCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.KeyCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.IKeyManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.SlotInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.util.KeyHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants._4k;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyBlockReadWrite implements IKeyBlockReadWrite {

    private static final Logger logger = LoggerFactory.getLogger(KeyBlockReadWrite.class);

    private final IKeyManifest keyManifest;
    private final FileReadWrite fileReadWrite = new FileReadWrite();

    private final LRUCache<String, byte[]> readCache;
    private final LRUCache<String, byte[]> writeCache;

    public KeyBlockReadWrite(IKeyManifest keyManifest) {
        this.keyManifest = keyManifest;
        this.readCache = new LRUCache<>(LRUCacheName.key_read_block_cache, _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        this.writeCache = new LRUCache<>(LRUCacheName.key_write_block_cache, _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        warm();
    }

    private String file(long fileId) {
        return FileNames.keyFile(keyManifest.dir(), fileId);
    }

    private void warm() {
        try {
            Set<Long> fileIds = keyManifest.getFileIds();
            for (Long fileId : fileIds) {
                try {
                    readBlocks(fileId, 0, _4k);
                } catch (Exception e) {
                    logger.error("warm error, fileId = {}", fileId, e);
                }
            }
        } catch (Exception e) {
            logger.error("warm error");
        }
    }

    @Override
    public KeyInfo get(short slot, Key key, CacheType cacheType) throws IOException {
        SlotInfo slotInfo = keyManifest.get(slot);
        if (slotInfo == null) {
            return null;
        }
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        int capacity = slotInfo.capacity();
        int bucketSize = capacity / _4k;
        int bucket = KeyHashUtils.hash(key.key()) % bucketSize;
        long bucketOffset = offset + bucket * _4k;

        String cacheKey = fileId + "|" + bucketOffset;
        byte[] block = readCache.get(cacheKey);
        if (block == null) {
            block = writeCache.get(cacheKey);
        }
        if (block == null) {
            block = fileReadWrite.read(file(fileId), bucketOffset, _4k);
            if (cacheType == CacheType.write) {
                writeCache.put(cacheKey, block);
            } else if (cacheType == CacheType.read) {
                readCache.put(cacheKey, block);
            }
        }
        Map<Key, KeyInfo> map = KeyCodec.decodeBucket(block);
        KeyInfo data = map.get(key);
        if (data == null) {
            return KeyInfo.DELETE;
        }
        return data;
    }

    @Override
    public void clearBlockCache(long fileId, long bucketOffset) {
        String cacheKey = fileId + "|" + bucketOffset;
        readCache.delete(cacheKey);
        writeCache.delete(cacheKey);
    }

    @Override
    public void updateBlockCache(long fileId, long bucketOffset, byte[] block) {
        if (block.length != _4k) {
            return;
        }
        String cacheKey = fileId + "|" + bucketOffset;
        byte[] cache = readCache.get(cacheKey);
        if (cache != null) {
            readCache.put(cacheKey, block);
        } else {
            writeCache.put(cacheKey, block);
        }
    }

    @Override
    public byte[] getBlock(long fileId, long bucketOffset) throws IOException {
        String cacheKey = fileId + "|" + bucketOffset;
        byte[] block = readCache.get(cacheKey);
        if (block == null) {
            block = writeCache.get(cacheKey);
        }
        if (block != null) {
            return block;
        }
        return fileReadWrite.read(file(fileId), bucketOffset, _4k);
    }

    @Override
    public void writeBlocks(long fileId, long offset, byte[] data) throws IOException {
        fileReadWrite.write(file(fileId), offset, data);
    }

    @Override
    public byte[] readBlocks(long fileId, long offset, int size) throws IOException {
        return fileReadWrite.read(file(fileId), offset, size);
    }

}

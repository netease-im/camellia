package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block;

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

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.*;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyBlockReadWrite implements IKeyBlockReadWrite {

    private static final Logger logger = LoggerFactory.getLogger(KeyBlockReadWrite.class);

    private static final String READ_CACHE_CONFIG_KEY = "local.storage.key.block.read.cache.capacity";
    private static final String WRITE_CACHE_CONFIG_KEY = "local.storage.key.block.write.cache.capacity";

    private final IKeyManifest keyManifest;
    private final FileReadWrite fileReadWrite = new FileReadWrite();

    private final LRUCache<String, byte[]> readCache;
    private final LRUCache<String, byte[]> writeCache;

    public KeyBlockReadWrite(IKeyManifest keyManifest) {
        this.keyManifest = keyManifest;
        this.readCache = new LRUCache<>("key-read-block-cache", READ_CACHE_CONFIG_KEY, "32M", _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        this.writeCache = new LRUCache<>("key-write-block-cache", WRITE_CACHE_CONFIG_KEY, "32M", _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
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
    public KeyInfo get(short slot, Key key) throws IOException {
        SlotInfo slotInfo = keyManifest.get(slot);
        if (slotInfo == null) {
            return null;
        }
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        int capacity = slotInfo.capacity();
        int bucketSize = capacity / _4k;
        int bucket = KeyHashUtils.hash(key.key()) % bucketSize;

        String cacheKey = fileId + "|" + offset;
        byte[] block = readCache.get(cacheKey);
        if (block == null) {
            block = writeCache.get(cacheKey);
            if (block != null) {
                readCache.put(cacheKey, block);
                writeCache.delete(cacheKey);
            }
        }
        if (block == null) {
            block = fileReadWrite.read(file(fileId), offset + bucket * _4k, _4k);
            readCache.put(cacheKey, block);
        }
        Map<Key, KeyInfo> map = KeyCodec.decodeBucket(block);
        KeyInfo data = map.get(key);
        if (data == null) {
            return KeyInfo.DELETE;
        }
        return data;
    }

    @Override
    public KeyInfo getForCompact(short slot, Key key) throws IOException {
        SlotInfo slotInfo = keyManifest.get(slot);
        if (slotInfo == null) {
            return null;
        }
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        int capacity = slotInfo.capacity();
        int bucketSize = capacity / _4k;
        int bucket = KeyHashUtils.hash(key.key()) % bucketSize;

        String cacheKey = fileId + "|" + offset;
        byte[] block = readCache.get(cacheKey);
        if (block == null) {
            block = writeCache.get(cacheKey);
        }
        if (block == null) {
            block = fileReadWrite.read(file(fileId), offset, bucket * _4k);
            writeCache.put(cacheKey, block);
        }
        Map<Key, KeyInfo> map = KeyCodec.decodeBucket(block);
        KeyInfo data = map.get(key);
        if (data == null) {
            return KeyInfo.DELETE;
        }
        return data;
    }

    @Override
    public void updateBlockCache(long fileId, long offset, byte[] block) {
        if (block.length != _4k) {
            return;
        }
        String cacheKey = fileId + "|" + offset;
        byte[] cache = readCache.get(cacheKey);
        if (cache != null) {
            readCache.put(cacheKey, block);
        } else {
            writeCache.put(cacheKey, block);
        }
    }

    @Override
    public byte[] getBlock(long fileId, long offset) throws IOException {
        String cacheKey = fileId + "|" + offset;
        byte[] block = readCache.get(cacheKey);
        if (block == null) {
            block = writeCache.get(cacheKey);
        }
        if (block != null) {
            return block;
        }
        return fileReadWrite.read(file(fileId), offset, _4k);
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

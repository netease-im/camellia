package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block;

import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageCacheMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCacheName;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.SizeCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValue;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValueCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValueDecodeResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileNames;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.file.FileReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockLocation;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.IValueManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.ValueLocation;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants._4k;

/**
 * Created by caojiajun on 2025/1/6
 */
public class StringBlockReadWrite implements IStringBlockReadWrite {

    private static final Logger logger = LoggerFactory.getLogger(StringBlockReadWrite.class);

    private final IValueManifest valueManifest;
    private final FileReadWrite fileReadWrite = FileReadWrite.getInstance();

    private final LRUCache<String, byte[]> readCache;
    private final LRUCache<String, byte[]> writeCache;

    public StringBlockReadWrite(IValueManifest valueManifest) {
        this.valueManifest = valueManifest;
        this.readCache = new LRUCache<>(LRUCacheName.string_read_block_cache, _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        this.writeCache = new LRUCache<>(LRUCacheName.string_write_block_cache, _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
    }

    private String file(BlockType blockType, long fileId) {
        return FileNames.stringBlockFile(valueManifest.dir(), blockType, fileId);
    }

    @Override
    public byte[] get(KeyInfo keyInfo) throws IOException {
        ValueLocation valueLocation = keyInfo.getValueLocation();
        if (valueLocation == null) {
            ErrorLogCollector.collect(StringBlockReadWrite.class, "value location is null");
            return null;
        }
        BlockLocation blockLocation = valueLocation.blockLocation();
        BlockType blockType = valueManifest.blockType(blockLocation.fileId());
        long fileId = blockLocation.fileId();
        long fileOffset = (long) blockType.getBlockSize() * blockLocation.blockId();
        String key = fileId + "|" + fileOffset;
        byte[] block = readCache.get(key);
        if (block == null) {
            block = writeCache.get(key);
            if (block != null) {
                readCache.put(key, block);
                writeCache.delete(key);
            }
        }
        if (block == null) {
            String file = file(blockType, fileId);
            block = fileReadWrite.read(file, fileOffset, blockType.getBlockSize());
            readCache.put(key, block);
            LocalStorageCacheMonitor.update(LocalStorageCacheMonitor.Type.disk, "string");
        } else {
            LocalStorageCacheMonitor.update(LocalStorageCacheMonitor.Type.block_cache, "string");
        }

        StringValueDecodeResult decodeResult = StringValueCodec.decode(block, blockType);
        List<byte[]> list = decodeResult.values();
        if (list.isEmpty()) {
            return null;
        }
        int offset = keyInfo.getValueLocation().offset();
        if (list.size() <= offset) {
            return null;
        }
        byte[] bytes = list.get(offset);
        StringValue stringValue = StringValue.decode(bytes);
        if (Arrays.equals(stringValue.key(), keyInfo.getKey())) {
            return stringValue.value();
        }
        return null;
    }

    @Override
    public void updateBlockCache(long fileId, long offset, byte[] block) {
        String key = fileId + "|" + offset;
        byte[] cache = readCache.get(key);
        if (cache != null) {
            readCache.put(key, block);
        } else {
            writeCache.put(key, block);
        }
    }

    @Override
    public void clearBlockCache(long fileId, long offset) {
        String key = fileId + "|" + offset;
        readCache.delete(key);
        writeCache.delete(key);
    }

    @Override
    public byte[] getBlock(BlockType blockType, long fileId, long offset) throws IOException {
        String cacheKey = fileId + "|" + offset;
        byte[] block = readCache.get(cacheKey);
        if (block == null) {
            block = writeCache.get(cacheKey);
        }
        if (block != null) {
            return block;
        }
        String file = file(blockType, fileId);
        block = fileReadWrite.read(file, offset, blockType.getBlockSize());
        writeCache.put(cacheKey, block);
        return block;
    }

    @Override
    public void writeBlocks(long fileId, long offset, byte[] data) throws IOException {
        BlockType blockType = valueManifest.blockType(fileId);
        String file = file(blockType, fileId);
        fileReadWrite.write(file, offset, data);
    }

    @Override
    public byte[] readBlocks(long fileId, long offset, int size) throws IOException {
        BlockType blockType = valueManifest.blockType(fileId);
        String file = file(blockType, fileId);
        return fileReadWrite.read(file, offset, size);
    }
}

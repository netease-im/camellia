package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCache;
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

    private static final String READ_CACHE_CONFIG_KEY = "local.storage.string.block.read.cache.capacity";
    private static final String WRITE_CACHE_CONFIG_KEY = "local.storage.string.block.write.cache.capacity";

    private final IValueManifest valueManifest;
    private final FileReadWrite fileReadWrite = new FileReadWrite();

    private final LRUCache<String, byte[]> readCache;
    private final LRUCache<String, byte[]> writeCache;

    public StringBlockReadWrite(IValueManifest valueManifest) {
        this.valueManifest = valueManifest;
        this.readCache = new LRUCache<>("string-read-block-cache", READ_CACHE_CONFIG_KEY, "32M", _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        this.writeCache = new LRUCache<>("string-write-block-cache", WRITE_CACHE_CONFIG_KEY, "32M", _4k, SizeCalculator.STRING_INSTANCE, SizeCalculator.BYTES_INSTANCE);
        warm();
    }

    private String file(BlockType blockType, long fileId) {
        return FileNames.stringBlockFile(valueManifest.dir(), blockType, fileId);
    }

    private void warm() {
        try {
            Map<Long, BlockType> fileIds = valueManifest.getFileIds();
            for (Map.Entry<Long, BlockType> entry : fileIds.entrySet()) {
                Long fileId = entry.getKey();
                BlockType blockType = entry.getValue();
                try {
                    readBlocks(fileId, 0, blockType.getBlockSize());
                } catch (Exception e) {
                    logger.error("warm error, fileId = {}, blockType = {}", fileId, blockType, e);
                }
            }
        } catch (Exception e) {
            logger.error("warm error", e);
        }
    }

    @Override
    public byte[] get(KeyInfo keyInfo) throws IOException {
        ValueLocation valueLocation = keyInfo.getValueLocation();
        if (valueLocation == null) {
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

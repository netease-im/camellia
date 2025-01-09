package com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheKey;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValueCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValueDecodeResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.*;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block.StringBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.StringReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValue;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/1/9
 */
public class CompactExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CompactExecutor.class);

    private KeyReadWrite keyReadWrite;
    private StringReadWrite stringReadWrite;

    private IValueManifest valueManifest;
    private StringBlockReadWrite stringBlockReadWrite;

    private final ConcurrentHashMap<Short, Long> lastCompactTimeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nextOffsetMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, BlockType> nextBlockTypeMap = new ConcurrentHashMap<>();

    public void compact(short slot) {
        Long lastCompactTime = lastCompactTimeMap.get(slot);
        if (lastCompactTime != null && TimeCache.currentMillis - lastCompactTime < 10*1000) {
            return;
        }
        try {
            BlockType blockType = nextBlockType(slot);
            int offset = nextOffset(blockType, slot);
            List<BlockLocation> blocks = valueManifest.getBlocks(slot, blockType, offset, 4);
            if (blocks.isEmpty()) {
                updateNextOffset(blockType, slot, 0);
                return;
            }
            List<Pair<KeyInfo, byte[]>> values = new ArrayList<>();
            List<BlockLocation> recycleBlocks = new ArrayList<>();

            for (BlockLocation block : blocks) {
                long fileId = block.fileId();
                int blockId = block.blockId();
                byte[] bytes = stringBlockReadWrite.getBlock(blockType, fileId, (long) blockId * blockType.getBlockSize());

                StringValueDecodeResult decodeResult = StringValueCodec.decode(bytes, blockType);
                List<byte[]> list = decodeResult.values();

                List<Pair<KeyInfo, byte[]>> surviving = new ArrayList<>();

                boolean recycle = false;
                for (byte[] data : list) {
                    StringValue stringValue = StringValue.decode(data);
                    KeyInfo keyInfo = keyReadWrite.getForCompact(slot, new CacheKey(stringValue.key()));
                    if (keyInfo == null) {
                        continue;
                    }
                    if (keyInfo.getValueLocation() != null) {
                        BlockLocation blockLocation = keyInfo.getValueLocation().blockLocation();
                        if (blockLocation.equals(block)) {
                            surviving.add(new Pair<>(keyInfo, stringValue.value()));
                        }
                    }
                }
                if (surviving.size() < list.size()) {
                    recycle = true;
                }
                if (!recycle) {
                    if (blockType == BlockType._4k) {
                        recycle = decodeResult.remaining() > 256;
                    } else if (blockType == BlockType._32k) {
                        recycle = decodeResult.remaining() > BlockType._4k.getBlockSize();
                    } else if (blockType == BlockType._256k) {
                        recycle = decodeResult.remaining() > BlockType._32k.getBlockSize();
                    } else if (blockType == BlockType._1024k) {
                        recycle = decodeResult.remaining() > BlockType._256k.getBlockSize();
                    }
                }
                if (recycle) {
                    values.addAll(surviving);
                    recycleBlocks.add(block);
                }
            }
            if (!values.isEmpty()) {
                for (Pair<KeyInfo, byte[]> pair : values) {
                    keyReadWrite.put(slot, pair.getFirst());
                    stringReadWrite.put(slot, pair.getFirst(), pair.getSecond());
                }
            }
            for (BlockLocation block : recycleBlocks) {
                valueManifest.recycle(slot, block);
            }
            if (recycleBlocks.isEmpty()) {
                updateNextOffset(blockType, slot, offset + 4);
            }
        } catch (Exception e) {
            logger.error("compact error, slot = {}", slot, e);
        } finally {
            lastCompactTimeMap.put(slot, TimeCache.currentMillis);
        }
    }

    private BlockType nextBlockType(short slot) {
        BlockType blockType = nextBlockTypeMap.get(slot);
        if (blockType == null) {
            blockType = BlockType._4k;
        }
        switch (blockType) {
            case _4k -> nextBlockTypeMap.put(slot, BlockType._32k);
            case _32k -> nextBlockTypeMap.put(slot, BlockType._256k);
            case _256k -> nextBlockTypeMap.put(slot, BlockType._1024k);
            case _1024k -> nextBlockTypeMap.put(slot, BlockType._4k);
        }
        return blockType;
    }

    private int nextOffset(BlockType blockType, short slot) {
        String key = blockType.getType() + "|" + slot;
        Integer nextOffset = nextOffsetMap.get(key);
        if (nextOffset == null) {
            return 0;
        }
        return nextOffset;
    }

    private void updateNextOffset(BlockType blockType, short slot, int nextOffset) {
        String key = blockType.getType() + "|" + slot;
        nextOffsetMap.put(key, nextOffset);
    }

}

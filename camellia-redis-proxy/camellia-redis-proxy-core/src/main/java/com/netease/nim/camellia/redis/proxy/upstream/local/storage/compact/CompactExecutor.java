package com.netease.nim.camellia.redis.proxy.upstream.local.storage.compact;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageTimeMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
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

    private final IValueManifest valueManifest;

    private final KeyReadWrite keyReadWrite;
    private final StringReadWrite stringReadWrite;
    private final StringBlockReadWrite stringBlockReadWrite;

    private int compactIntervalSeconds;
    private Map<BlockType, Integer> blockLimit;

    private final ConcurrentHashMap<Short, Long> lastCompactTimeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nextOffsetMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, BlockType> nextBlockTypeMap = new ConcurrentHashMap<>();

    public CompactExecutor(LocalStorageReadWrite readWrite) {
        this.valueManifest = readWrite.getValueManifest();
        this.keyReadWrite = readWrite.getKeyReadWrite();
        this.stringReadWrite = readWrite.getStringReadWrite();
        this.stringBlockReadWrite = readWrite.getStringBlockReadWrite();
        updateConf();
        ProxyDynamicConf.registerCallback(this::updateConf);
    }

    public void compact(short slot) {
        Long lastCompactTime = lastCompactTimeMap.get(slot);
        if (lastCompactTime != null && TimeCache.currentMillis - lastCompactTime < compactIntervalSeconds*1000L) {
            return;
        }
        long startTime = System.nanoTime();
        BlockType blockType = nextBlockType(slot);
        int offset = nextOffset(blockType, slot);
        int limit = blockLimit.getOrDefault(blockType, 2);
        try {
            List<BlockLocation> blocks = valueManifest.getBlocks(slot, blockType, offset, limit);
            if (blocks.isEmpty() || blocks.size() == 1) {
                updateNextOffset(blockType, slot, 0);
                return;
            }

            int remaining = 0;
            List<Pair<KeyInfo, byte[]>> surviving = new ArrayList<>();

            for (BlockLocation blockLocation : blocks) {
                long fileId = blockLocation.fileId();
                int blockId = blockLocation.blockId();
                byte[] block = stringBlockReadWrite.getBlock(blockType, fileId, (long) blockId * blockType.getBlockSize());

                StringValueDecodeResult decodeResult = StringValueCodec.decode(block, blockType);

                List<byte[]> list = decodeResult.values();

                int survivingCount = 0;
                for (byte[] data : list) {
                    StringValue stringValue = StringValue.decode(data);
                    KeyInfo keyInfo = keyReadWrite.getForCompact(slot, new Key(stringValue.key()));
                    if (keyInfo == null) {
                        continue;
                    }
                    if (keyInfo.getValueLocation() != null) {
                        if (keyInfo.getValueLocation().blockLocation().equals(blockLocation)) {
                            surviving.add(new Pair<>(keyInfo, stringValue.value()));
                            survivingCount ++;
                        }
                    }
                }

                long used = blockType.getBlockSize() - decodeResult.remaining();
                if (list.isEmpty()) {
                    remaining = blockType.getBlockSize();
                } else {
                    remaining += (int) (used * survivingCount / list.size());
                    remaining += decodeResult.remaining();
                }
            }

            if (remaining < blockType.getBlockSize()) {
                return;
            }

            if (!surviving.isEmpty()) {
                for (Pair<KeyInfo, byte[]> pair : surviving) {
                    stringReadWrite.put(slot, pair.getFirst(), pair.getSecond());
                    keyReadWrite.put(slot, pair.getFirst());
                }
            }
            for (BlockLocation block : blocks) {
                valueManifest.recycle(slot, block);
            }
            updateNextOffset(blockType, slot, offset + limit);
        } catch (Exception e) {
            logger.error("compact error, slot = {}, blockType = {}, offset = {}, limit = {}", slot, blockType, offset, limit, e);
        } finally {
            lastCompactTimeMap.put(slot, TimeCache.currentMillis);
            LocalStorageTimeMonitor.time("compact", System.nanoTime() - startTime);
        }
    }

    private void updateConf() {
        compactIntervalSeconds = ProxyDynamicConf.getInt("local.storage.compact.interval.seconds", 10);
        Map<BlockType, Integer> blockLimit = new HashMap<>();
        for (BlockType type : BlockType.values()) {
            String key = "local.storage.compact.block.type." + type.getType() + ".limit";
            if (type == BlockType._4k) {
                blockLimit.put(type, ProxyDynamicConf.getInt(key, 6));
            } else {
                blockLimit.put(type, ProxyDynamicConf.getInt(key, 3));
            }
        }
        this.blockLimit = blockLimit;
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
            case _1024k -> nextBlockTypeMap.put(slot, BlockType._8m);
            case _8m -> nextBlockTypeMap.put(slot, BlockType._4k);
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

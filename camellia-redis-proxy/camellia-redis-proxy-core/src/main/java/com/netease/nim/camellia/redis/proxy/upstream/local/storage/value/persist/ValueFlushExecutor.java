package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist;

import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValueCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValueEncodeResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.*;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block.StringBlockReadWrite;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/6
 */
public class ValueFlushExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ValueFlushExecutor.class);

    private final FlushExecutor executor;
    private final IValueManifest valueManifest;
    private final StringBlockReadWrite stringBlockReadWrite;

    public ValueFlushExecutor(FlushExecutor executor, IValueManifest valueManifest, StringBlockReadWrite blockCache) {
        this.executor = executor;
        this.valueManifest = valueManifest;
        this.stringBlockReadWrite = blockCache;
    }

    public CompletableFuture<FlushResult> submit(StringValueFlushTask flushTask) {
        CompletableFuture<FlushResult> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                long startTime = System.nanoTime();
                try {
                    execute(flushTask);
                    future.complete(FlushResult.OK);
                } catch (Exception e) {
                    logger.error("string value flush error, slot = {}", flushTask.slot(), e);
                    future.complete(FlushResult.ERROR);
                } finally {
                    LocalStorageMonitor.valueFlushTime(System.nanoTime() - startTime);
                }
            });
        } catch (Exception e) {
            logger.error("submit string value flush error, slot = {}", flushTask.slot(), e);
            future.complete(FlushResult.ERROR);
        }
        return future;
    }

    private void execute(StringValueFlushTask task) throws Exception {
        short slot = task.slot();
        Map<Key, byte[]> flushValues = task.flushValues();
        Map<BlockType, List<Pair<KeyInfo, byte[]>>> blockMap = new HashMap<>();

        Map<Key, KeyInfo> keyMap = task.keyMap();
        for (Map.Entry<Key, byte[]> entry : flushValues.entrySet()) {
            byte[] data = entry.getValue();
            BlockType blockType = BlockType.fromData(data);
            KeyInfo keyInfo = keyMap.get(entry.getKey());
            if (keyInfo == null || keyInfo.isExpire() || keyInfo == KeyInfo.DELETE) {
                continue;
            }
            List<Pair<KeyInfo, byte[]>> buffers = blockMap.computeIfAbsent(blockType, k -> new ArrayList<>());
            buffers.add(new Pair<>(keyInfo, entry.getValue()));
        }
        List<BlockInfo> list = new ArrayList<>();
        for (Map.Entry<BlockType, List<Pair<KeyInfo, byte[]>>> entry : blockMap.entrySet()) {
            StringValueEncodeResult result = StringValueCodec.encode(slot, entry.getKey(), valueManifest, entry.getValue());
            list.addAll(result.blockInfos());
        }
        for (BlockInfo blockInfo : list) {
            BlockLocation blockLocation = blockInfo.blockLocation();
            long fileId = blockLocation.fileId();
            long offset = (long) blockLocation.blockId() * blockInfo.blockType().getBlockSize();
            stringBlockReadWrite.writeBlocks(fileId, offset, blockInfo.data());
            stringBlockReadWrite.updateBlockCache(fileId, offset, blockInfo.data());
            valueManifest.commit(slot, blockLocation);
        }
    }

}

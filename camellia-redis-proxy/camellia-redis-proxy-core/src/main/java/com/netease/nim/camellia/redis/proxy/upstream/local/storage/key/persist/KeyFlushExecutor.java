package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.KeyCodec;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.IKeyManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.util.KeyHashUtils;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block.KeyBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.SlotInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.*;

/**
 * Created by caojiajun on 2025/1/2
 */
public class KeyFlushExecutor {

    private static final Logger logger = LoggerFactory.getLogger(KeyFlushExecutor.class);

    private final FlushExecutor executor;
    private final IKeyManifest keyManifest;
    private final KeyBlockReadWrite keyBlockReadWrite;

    public KeyFlushExecutor(FlushExecutor executor, IKeyManifest keyManifest, KeyBlockReadWrite keyBlockReadWrite) {
        this.executor = executor;
        this.keyManifest = keyManifest;
        this.keyBlockReadWrite = keyBlockReadWrite;
    }

    public CompletableFuture<FlushResult> submit(KeyFlushTask flushTask) {
        CompletableFuture<FlushResult> future = new CompletableFuture<>();
        try {
            executor.submit(() -> {
                try {
                    execute(flushTask);
                    future.complete(FlushResult.OK);
                } catch (Exception e) {
                    logger.error("key flush error, slot = {}", flushTask.slot(), e);
                    future.complete(FlushResult.ERROR);
                }
            });
        } catch (Exception e) {
            logger.error("submit key flush error, slot = {}", flushTask.slot(), e);
            future.complete(FlushResult.ERROR);
        }
        return future;
    }

    private void execute(KeyFlushTask task) throws Exception {
        short slot = task.slot();
        Map<Key, KeyInfo> flushKeys = task.flushKeys();
        SlotInfo source = keyManifest.get(slot);
        if (source == null) {
            source = keyManifest.init(slot);
            clear(source);
        }
        SlotInfo target = source;
        WriteResult lastWrite = null;
        while (true) {
            lastWrite = writeTo(source, target, flushKeys, lastWrite);
            if (lastWrite.success) {
                break;
            }
            target = keyManifest.expand(slot);
        }
    }

    private void clear(SlotInfo slotInfo) throws IOException {
        long fileId = slotInfo.fileId();
        long offset = slotInfo.offset();
        int capacity = slotInfo.capacity();
        int bucketSize = capacity / _4k;
        for (int i=0; i<bucketSize; i++) {
            keyBlockReadWrite.clearBlockCache(fileId, offset + i * _4k);
        }
        keyBlockReadWrite.writeBlocks(fileId, offset, new byte[capacity]);
    }

    private WriteResult writeTo(SlotInfo source, SlotInfo target, Map<Key, KeyInfo> flushKeys, WriteResult lastWrite) throws IOException {
        Map<Integer, Map<Key, KeyInfo>> writeBuffer = new HashMap<>();
        int capacity = target.capacity();
        int bucketSize = capacity / _4k;
        {
            for (Map.Entry<Key, KeyInfo> entry : flushKeys.entrySet()) {
                Key key = entry.getKey();
                KeyInfo data = entry.getValue();
                int bucket = KeyHashUtils.hash(key.key()) % bucketSize;
                Map<Key, KeyInfo> keys = writeBuffer.computeIfAbsent(bucket, k -> new HashMap<>());
                keys.put(key, data);
            }
        }
        boolean expand = false;
        WriteResult writeResult = new WriteResult();

        TreeMap<Long, WriteTask> tasks = new TreeMap<>();
        if (source.equals(target)) {
            long fileId = source.fileId();
            long offset = source.offset();
            for (Map.Entry<Integer, Map<Key, KeyInfo>> entry : writeBuffer.entrySet()) {
                Integer bucket = entry.getKey();
                Map<Key, KeyInfo> newKeys = entry.getValue();
                long bucketOffset = offset + bucket * _4k;
                Map<Key, KeyInfo> oldKeys = null;
                if (lastWrite != null) {
                    oldKeys = lastWrite.oldBucketKeys.get(bucket);
                }
                if (oldKeys == null) {
                    byte[] bytes = keyBlockReadWrite.getBlock(fileId, bucketOffset);
                    oldKeys = KeyCodec.decodeBucket(bytes);
                }
                writeResult.oldBucketKeys.put(bucket, oldKeys);
                merge(newKeys, oldKeys);
                byte[] encoded = KeyCodec.encodeBucket(newKeys);
                if (encoded == null) {
                    expand = true;
                    break;
                }
                tasks.put(bucketOffset, new WriteTask(bucketOffset, encoded));
            }
        } else {
            Map<Key, KeyInfo> oldAllKeys;
            if (lastWrite == null || lastWrite.oldAllKeys == null) {
                byte[] oldAll = keyBlockReadWrite.readBlocks(source.fileId(), source.offset(), source.capacity());
                oldAllKeys = KeyCodec.decodeSlot(oldAll);
            } else {
                oldAllKeys = lastWrite.oldAllKeys;
            }
            for (Map.Entry<Key, KeyInfo> entry : oldAllKeys.entrySet()) {
                int bucket = KeyHashUtils.hash(entry.getKey().key()) % bucketSize;
                Map<Key, KeyInfo> newKeys = writeBuffer.computeIfAbsent(bucket, k -> new HashMap<>());
                merge(newKeys, entry);
            }
            for (Map.Entry<Integer, Map<Key, KeyInfo>> entry : writeBuffer.entrySet()) {
                Integer bucket = entry.getKey();
                byte[] encoded = KeyCodec.encodeBucket(entry.getValue());
                if (encoded == null) {
                    expand = true;
                    break;
                }
                long bucketOffset = target.offset() + bucket * _4k;
                tasks.put(bucketOffset, new WriteTask(bucketOffset, encoded));
            }
            writeResult.oldAllKeys = oldAllKeys;
        }
        if (!expand) {
            write0(target.fileId(), tasks);
            writeResult.success = true;
            return writeResult;
        }
        writeResult.success = false;
        return writeResult;
    }

    private static class WriteResult {
        boolean success;
        Map<Key, KeyInfo> oldAllKeys;
        Map<Integer, Map<Key, KeyInfo>> oldBucketKeys = new HashMap<>();
    }

    private static class WriteTask {
        long offset;
        byte[] data;

        public WriteTask(long offset, byte[] data) {
            this.offset = offset;
            this.data = data;
        }
    }

    private void write0(long fileId, TreeMap<Long, WriteTask> writeTasks) throws IOException {
        List<List<WriteTask>> all = new ArrayList<>();
        List<WriteTask> merged = new ArrayList<>();
        WriteTask lastTask = null;
        for (Map.Entry<Long, WriteTask> entry : writeTasks.entrySet()) {
            Long offset = entry.getKey();
            WriteTask task = entry.getValue();
            //update block cache
            keyBlockReadWrite.updateBlockCache(fileId, task.offset, task.data);
            //merge
            if (lastTask != null) {
                if (offset - lastTask.offset != _4k) {
                    if (!merged.isEmpty()) {
                        all.add(merged);
                        merged = new ArrayList<>();
                    }
                }
            }
            merged.add(task);
            lastTask = task;
        }
        if (!merged.isEmpty()) {
            all.add(merged);
        }
        for (List<WriteTask> tasks : all) {
            if (tasks.size() == 1) {
                WriteTask first = tasks.getFirst();
                keyBlockReadWrite.writeBlocks(fileId, first.offset, first.data);
            } else {
                byte[] mergedData = new byte[_4k * tasks.size()];
                long offset = tasks.getFirst().offset;
                for (int i=0;i<tasks.size(); i++) {
                    System.arraycopy(tasks.get(i).data, 0, mergedData, _4k * i, _4k);
                }
                keyBlockReadWrite.writeBlocks(fileId, offset, mergedData);
            }
        }
    }

    private void merge(Map<Key, KeyInfo> newKeys, Map.Entry<Key, KeyInfo> entry) {
        KeyInfo key = newKeys.get(entry.getKey());
        if (key == KeyInfo.DELETE) {
            return;
        }
        if (!newKeys.containsKey(entry.getKey())) {
            newKeys.put(entry.getKey(), entry.getValue());
        }
    }

    private void merge(Map<Key, KeyInfo> newKeys, Map<Key, KeyInfo> oldKeys) {
        for (Map.Entry<Key, KeyInfo> entry : oldKeys.entrySet()) {
            merge(newKeys, entry);
        }
    }
}

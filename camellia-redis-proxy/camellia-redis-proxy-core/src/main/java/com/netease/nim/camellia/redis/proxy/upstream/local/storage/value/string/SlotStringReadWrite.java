package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageCacheMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.LocalStorageTimeMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec.StringValue;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushStatus;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.StringValueFlushTask;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.ValueFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block.StringBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/3
 */
public class SlotStringReadWrite {

    private static final Logger logger = LoggerFactory.getLogger(SlotStringReadWrite.class);

    private static int stringValueFlushSize;
    private static int stringValueFlushIntervalSeconds;
    static {
        updateConf();
        ProxyDynamicConf.registerCallback(SlotStringReadWrite::updateConf);
    }
    private static void updateConf() {
        int stringValueFlushSize = ProxyDynamicConf.getInt("local.storage.string.value.flush.size", 128);
        if (SlotStringReadWrite.stringValueFlushSize != stringValueFlushSize) {
            SlotStringReadWrite.stringValueFlushSize = stringValueFlushSize;
            logger.info("local.storage.string.value.flush.size, update to {}", stringValueFlushSize);
        }
        int stringValueFlushIntervalSeconds = ProxyDynamicConf.getInt("local.storage.string.value.flush.interval.seconds", 300);
        if (SlotStringReadWrite.stringValueFlushIntervalSeconds != stringValueFlushIntervalSeconds) {
            SlotStringReadWrite.stringValueFlushIntervalSeconds = stringValueFlushIntervalSeconds;
            logger.info("local.storage.string.value.flush.interval.seconds, update to {}", stringValueFlushIntervalSeconds);
        }
    }

    private long lastFlushTime = TimeCache.currentMillis;

    private final short slot;
    private final StringBlockReadWrite stringBlockReadWrite;
    private final ValueFlushExecutor flushExecutor;

    private final Map<KeyInfo, byte[]> mutable = new HashMap<>();
    private final Map<KeyInfo, byte[]> immutable = new HashMap<>();
    private volatile FlushStatus flushStatus = FlushStatus.FLUSH_OK;

    private CompletableFuture<Boolean> flushFuture;

    public SlotStringReadWrite(short slot, ValueFlushExecutor flushExecutor, StringBlockReadWrite slotStringBlockCache) {
        this.slot = slot;
        this.flushExecutor = flushExecutor;
        this.stringBlockReadWrite = slotStringBlockCache;
        updateConf();
    }

    /**
     * put
     * @param keyInfo key info
     * @param data data
     * @throws IOException exception
     */
    public void put(KeyInfo keyInfo, byte[] data) throws IOException {
        mutable.put(keyInfo, data);
        if (mutable.size() >= stringValueFlushSize * 2) {
            waitForFlush();
        }
    }

    /**
     * delete
     * @param keyInfo key info
     * @throws IOException exception
     */
    public void delete(KeyInfo keyInfo) throws IOException {
        mutable.remove(keyInfo);
    }

    /**
     * get
     * @param keyInfo key info
     * @return data
     * @throws IOException exception
     */
    public byte[] get(KeyInfo keyInfo) throws IOException {
        byte[] data = mutable.get(keyInfo);
        if (data != null) {
            LocalStorageCacheMonitor.update(LocalStorageCacheMonitor.Type.mem_table, "string");
            return data;
        }
        data = immutable.get(keyInfo);
        if (data != null) {
            LocalStorageCacheMonitor.update(LocalStorageCacheMonitor.Type.mem_table, "string");
            return data;
        }
        return stringBlockReadWrite.get(keyInfo);
    }

    /**
     * get for run to completion
     * @param keyInfo key info
     * @return data wrapper
     */
    public ValueWrapper<byte[]> getForRunToCompletion(KeyInfo keyInfo) {
        byte[] bytes1 = mutable.get(keyInfo);
        if (bytes1 != null) {
            LocalStorageCacheMonitor.update(LocalStorageCacheMonitor.Type.mem_table, "string");
            return () -> bytes1;
        }
        byte[] bytes2 = immutable.get(keyInfo);
        if (bytes2 != null) {
            LocalStorageCacheMonitor.update(LocalStorageCacheMonitor.Type.mem_table, "string");
            return () -> bytes2;
        }
        return null;
    }

    /**
     * flush value to disk
     * @param keyMap key map
     * @return result with future
     * @throws IOException exception
     */
    public CompletableFuture<FlushResult> flush(Map<Key, KeyInfo> keyMap) throws IOException {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return CompletableFuture.completedFuture(FlushResult.SKIP);
        }
        if (mutable.isEmpty()) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        flushFuture = new CompletableFuture<>();
        CompletableFuture<FlushResult> future = new CompletableFuture<>();
        Map<Key, byte[]> encodeMap = StringValue.encodeMap(mutable);
        immutable.putAll(mutable);
        mutable.clear();
        flushStatus = FlushStatus.FLUSHING;
        CompletableFuture<FlushResult> submit = flushExecutor.submit(new StringValueFlushTask(slot, encodeMap, keyMap));
        submit.thenAccept(flushResult -> {
            flushDone();
            future.complete(flushResult);
        });
        return future;
    }

    /**
     * check need flush
     * @return true/false
     */
    public boolean needFlush() {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return false;
        }
        return mutable.size() >= stringValueFlushSize || TimeCache.currentMillis - lastFlushTime > stringValueFlushIntervalSeconds*1000L;
    }

    private void flushDone() {
        immutable.clear();
        flushFuture.complete(true);
        flushFuture = null;
        flushStatus = FlushStatus.FLUSH_OK;
        lastFlushTime = TimeCache.currentMillis;
    }

    private void waitForFlush() {
        if (flushFuture != null) {
            long startTime = System.nanoTime();
            try {
                flushFuture.get();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                LocalStorageTimeMonitor.time("string_value_wait_flush", System.nanoTime() - startTime);
            }
        }
    }
}

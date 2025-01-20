package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
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

    private long lastFlushTime = TimeCache.currentMillis;

    private final short slot;
    private final StringBlockReadWrite stringBlockReadWrite;
    private final ValueFlushExecutor flushExecutor;

    private final Map<KeyInfo, byte[]> mutable = new HashMap<>();
    private final Map<KeyInfo, byte[]> immutable = new HashMap<>();
    private volatile FlushStatus flushStatus = FlushStatus.FLUSH_OK;

    private CompletableFuture<FlushResult> flushFuture;

    private int stringValueFlushSize;
    private int stringValueFlushIntervalSeconds;

    public SlotStringReadWrite(short slot, ValueFlushExecutor flushExecutor, StringBlockReadWrite slotStringBlockCache) {
        this.slot = slot;
        this.flushExecutor = flushExecutor;
        this.stringBlockReadWrite = slotStringBlockCache;
        updateConf();
        ProxyDynamicConf.registerCallback(this::updateConf);
    }

    private void updateConf() {
        int stringValueFlushSize = ProxyDynamicConf.getInt("local.storage.string.value.flush.size", 200);
        if (this.stringValueFlushSize != stringValueFlushSize) {
            this.stringValueFlushSize = stringValueFlushSize;
            logger.info("local.storage.string.value.flush.size, update to {}", stringValueFlushSize);
        }
        int stringValueFlushIntervalSeconds = ProxyDynamicConf.getInt("local.storage.string.value.flush.interval.seconds", 300);
        if (this.stringValueFlushIntervalSeconds != stringValueFlushIntervalSeconds) {
            this.stringValueFlushIntervalSeconds = stringValueFlushIntervalSeconds;
            logger.info("local.storage.string.value.flush.interval.seconds, update to {}", stringValueFlushIntervalSeconds);
        }
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
            return data;
        }
        data = immutable.get(keyInfo);
        if (data != null) {
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
            return () -> bytes1;
        }
        byte[] bytes2 = immutable.get(keyInfo);
        if (bytes2 != null) {
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
        this.flushFuture = submit;
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
        flushFuture = null;
        flushStatus = FlushStatus.FLUSH_OK;
        lastFlushTime = TimeCache.currentMillis;
    }

    private void waitForFlush() {
        if (flushFuture != null) {
            try {
                flushFuture.get();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

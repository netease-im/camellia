package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/3
 */
public class SlotStringReadWrite {

    private long lastFlushTime = TimeCache.currentMillis;

    private final short slot;
    private final StringBlockReadWrite stringBlockReadWrite;
    private final ValueFlushExecutor flushExecutor;

    private final Map<KeyInfo, byte[]> mutable = new HashMap<>();
    private final Map<KeyInfo, byte[]> immutable = new HashMap<>();
    private volatile FlushStatus flushStatus = FlushStatus.FLUSH_OK;

    public SlotStringReadWrite(short slot, ValueFlushExecutor flushExecutor, StringBlockReadWrite slotStringBlockCache) {
        this.slot = slot;
        this.flushExecutor = flushExecutor;
        this.stringBlockReadWrite = slotStringBlockCache;
    }

    public void put(KeyInfo keyInfo, byte[] data) throws IOException {
        mutable.put(keyInfo, data);
    }

    public void delete(KeyInfo keyInfo) throws IOException {
        mutable.remove(keyInfo);
    }

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
        return future;
    }

    public boolean needFlush() {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return false;
        }
        return mutable.size() >= 200 || TimeCache.currentMillis - lastFlushTime > 10*1000;
    }

    private void flushDone() {
        immutable.clear();
        flushStatus = FlushStatus.FLUSH_OK;
        lastFlushTime = TimeCache.currentMillis;
    }
}

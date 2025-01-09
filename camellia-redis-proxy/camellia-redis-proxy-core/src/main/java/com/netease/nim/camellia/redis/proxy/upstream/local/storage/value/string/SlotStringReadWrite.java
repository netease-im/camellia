package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushStatus;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.StringValueFlushTask;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.ValueFlushExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2025/1/3
 */
public class SlotStringReadWrite {

    private final short slot;
    private final StringBlockCache slotStringBlockCache;
    private final ValueFlushExecutor flushExecutor;

    private final Map<KeyInfo, byte[]> mutable = new HashMap<>();
    private final Map<KeyInfo, byte[]> immutable = new HashMap<>();
    private volatile FlushStatus flushStatus = FlushStatus.FLUSH_OK;

    public SlotStringReadWrite(short slot, ValueFlushExecutor flushExecutor, StringBlockCache slotStringBlockCache) {
        this.slot = slot;
        this.flushExecutor = flushExecutor;
        this.slotStringBlockCache = slotStringBlockCache;
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
        return slotStringBlockCache.get(slot, keyInfo);
    }

    public CompletableFuture<FlushResult> flush() throws IOException {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return CompletableFuture.completedFuture(FlushResult.SKIP);
        }
        if (mutable.isEmpty()) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        CompletableFuture<FlushResult> future = new CompletableFuture<>();
        immutable.putAll(mutable);
        mutable.clear();
        flushStatus = FlushStatus.FLUSHING;
        CompletableFuture<FlushResult> submit = flushExecutor.submit(new StringValueFlushTask(slot, immutable));
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
        return mutable.size() >= 200;
    }

    private void flushDone() {
        immutable.clear();
        flushStatus = FlushStatus.FLUSH_OK;
    }
}

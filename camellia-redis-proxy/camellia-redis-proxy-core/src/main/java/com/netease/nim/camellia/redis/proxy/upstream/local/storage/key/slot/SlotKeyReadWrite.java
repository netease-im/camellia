package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheKey;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushStatus;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block.KeyBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 线程不安全
 * Created by caojiajun on 2025/1/2
 */
public class SlotKeyReadWrite {

    private static final Logger logger = LoggerFactory.getLogger(SlotKeyReadWrite.class);

    private final short slot;
    private final KeyFlushExecutor executor;
    private final KeyBlockReadWrite blockCache;

    private final Map<CacheKey, KeyInfo> mutable = new HashMap<>();
    private final Map<CacheKey, KeyInfo> immutable = new HashMap<>();

    private volatile FlushStatus flushStatus = FlushStatus.FLUSH_OK;

    public SlotKeyReadWrite(short slot, KeyFlushExecutor executor, KeyBlockReadWrite blockCache) {
        this.slot = slot;
        this.executor = executor;
        this.blockCache = blockCache;
    }

    /**
     * 获取一个key
     * @param key key
     * @return key
     */
    public KeyInfo get(CacheKey key) throws IOException  {
        KeyInfo keyInfo = mutable.get(key);
        if (keyInfo == KeyInfo.DELETE) {
            return null;
        }
        keyInfo = immutable.get(key);
        if (keyInfo == KeyInfo.DELETE) {
            return null;
        }
        keyInfo = blockCache.get(slot, key);
        if (keyInfo == KeyInfo.DELETE) {
            return null;
        }
        return keyInfo;
    }

    /**
     * 写入一个key
     * @param key key
     */
    public void put(KeyInfo key) {
        mutable.put(new CacheKey(key.getKey()), key);
    }

    /**
     * 删除一个key
     * @param key key
     */
    public void delete(CacheKey key) {
        mutable.put(key, KeyInfo.DELETE);
    }

    /**
     * flush
     */
    public CompletableFuture<FlushResult> flush() {
        CompletableFuture<FlushResult> future = new CompletableFuture<>();
        if (flushStatus == FlushStatus.FLUSHING || flushStatus == FlushStatus.FLUSH_OK) {
            future.complete(FlushResult.SKIP);
            return future;
        }
        flushStatus = FlushStatus.FLUSHING;
        CompletableFuture<FlushResult> submit = executor.submit(new KeyFlushTask(slot, immutable));
        submit.thenAccept(b -> {
            flushDone();
            future.complete(b);
        });
        return future;
    }

    /**
     * flush prepare
     */
    public void flushPrepare() {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return;
        }
        if (mutable.isEmpty()) {
            return;
        }
        immutable.putAll(mutable);
        mutable.clear();
        flushStatus = FlushStatus.PREPARE;
    }

    /**
     * check need flush
     * @return true/false
     */
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

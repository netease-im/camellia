package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.slot;

import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.persist.KeyFlushTask;
import com.netease.nim.camellia.tools.utils.BytesKey;
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
    private final SlotKeyBlockCache blockCache;

    private Map<BytesKey, KeyInfo> mutable = new HashMap<>();
    private Map<BytesKey, KeyInfo> immutable = new HashMap<>();
    private volatile boolean flushing = false;

    public SlotKeyReadWrite(short slot, KeyFlushExecutor executor, SlotKeyBlockCache blockCache) {
        this.slot = slot;
        this.executor = executor;
        this.blockCache = blockCache;
    }

    /**
     * 获取一个key
     * @param key key
     * @return key
     */
    public KeyInfo get(BytesKey key) throws IOException  {
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
        mutable.put(new BytesKey(key.getKey()), key);
        checkAndFlush();
    }

    /**
     * 删除一个key
     * @param key key
     */
    public void delete(BytesKey key) {
        mutable.put(key, KeyInfo.DELETE);
        checkAndFlush();
    }

    /**
     * flush
     */
    public CompletableFuture<FlushResult> flush() {
        CompletableFuture<FlushResult> future = new CompletableFuture<>();
        if (flushing) {
            future.complete(FlushResult.SKIP);
            return future;
        }
        if (mutable.isEmpty()) {
            future.complete(FlushResult.OK);
            return future;
        }
        Map<BytesKey, KeyInfo> flushKeys = flushPrepare();
        CompletableFuture<FlushResult> submit = executor.submit(new KeyFlushTask(slot, flushKeys));
        submit.thenAccept(b -> {
            flushDone();
            future.complete(b);
        });
        return future;
    }

    private Map<BytesKey, KeyInfo> flushPrepare() {
        immutable = mutable;
        mutable = new HashMap<>();
        flushing = true;
        return immutable;
    }

    private void flushDone() {
        immutable.clear();
        flushing = false;
    }

    private void checkAndFlush() {
        if (mutable.size() >= 200 && !flushing) {
            flush();
        }
    }
}

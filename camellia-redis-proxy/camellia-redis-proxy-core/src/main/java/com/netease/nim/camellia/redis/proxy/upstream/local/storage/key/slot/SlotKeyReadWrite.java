package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushStatus;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block.KeyBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushTask;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
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

    private long lastFlushTime = TimeCache.currentMillis;

    private final short slot;
    private final KeyFlushExecutor executor;
    private final KeyBlockReadWrite keyBlockReadWrite;

    private final Map<Key, KeyInfo> mutable = new HashMap<>();
    private final Map<Key, KeyInfo> immutable = new HashMap<>();

    private volatile FlushStatus flushStatus = FlushStatus.FLUSH_OK;

    public SlotKeyReadWrite(short slot, KeyFlushExecutor executor, KeyBlockReadWrite keyBlockReadWrite) {
        this.slot = slot;
        this.executor = executor;
        this.keyBlockReadWrite = keyBlockReadWrite;
    }

    /**
     * 获取一个key
     * @param key key
     * @return key
     */
    public KeyInfo get(Key key) throws IOException  {
        return get0(CacheType.read, key);
    }

    /**
     * 获取一个key
     * @param key key
     * @return key
     */
    public KeyInfo getForCompact(Key key) throws IOException  {
        return get0(CacheType.write, key);
    }

    /**
     * 获取一个key
     * @param key key
     * @return key
     */
    public ValueWrapper<KeyInfo> getForRunToCompletion(Key key) {
        KeyInfo keyInfo1 = mutable.get(key);
        if (keyInfo1 == KeyInfo.DELETE) {
            return () -> null;
        }
        KeyInfo keyInfo2 = immutable.get(key);
        if (keyInfo2 == KeyInfo.DELETE) {
            return () -> null;
        }
        if (keyInfo2 != null) {
            return () -> keyInfo2;
        }
        return null;
    }

    /**
     * 写入一个key
     * @param key key
     */
    public void put(KeyInfo key) {
        mutable.put(new Key(key.getKey()), key);
    }

    /**
     * 删除一个key
     * @param key key
     */
    public void delete(Key key) {
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
    public Map<Key, KeyInfo> flushPrepare() {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return Collections.emptyMap();
        }
        if (mutable.isEmpty()) {
            return Collections.emptyMap();
        }
        immutable.putAll(mutable);
        mutable.clear();
        flushStatus = FlushStatus.PREPARE;
        return immutable;
    }

    /**
     * check need flush
     * @return true/false
     */
    public boolean needFlush() {
        if (flushStatus != FlushStatus.FLUSH_OK) {
            return false;
        }
        return mutable.size() >= 200 || TimeCache.currentMillis - lastFlushTime > 600*1000L;
    }

    private KeyInfo get0(CacheType cacheType, Key key) throws IOException {
        KeyInfo keyInfo = mutable.get(key);
        if (keyInfo == KeyInfo.DELETE) {
            return null;
        }
        if (keyInfo != null) {
            return keyInfo;
        }
        keyInfo = immutable.get(key);
        if (keyInfo == KeyInfo.DELETE) {
            return null;
        }
        if (keyInfo != null) {
            return keyInfo.duplicate();
        }
        keyInfo = keyBlockReadWrite.get(slot, key, cacheType);
        if (keyInfo == KeyInfo.DELETE) {
            return null;
        }
        return keyInfo;
    }

    private void flushDone() {
        immutable.clear();
        flushStatus = FlushStatus.FLUSH_OK;
        lastFlushTime = TimeCache.currentMillis;
    }
}

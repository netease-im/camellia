package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.CacheKey;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.EstimateSizeValueCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block.KeyBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.SlotKeyReadWrite;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyReadWrite {

    private final KeyFlushExecutor executor;
    private final KeyBlockReadWrite blockCache;

    private final ConcurrentHashMap<Short, SlotKeyReadWrite> map = new ConcurrentHashMap<>();

    private final LRUCache<CacheKey, KeyInfo> cache;

    public KeyReadWrite(KeyFlushExecutor executor, KeyBlockReadWrite blockCache) {
        this.executor = executor;
        this.blockCache = blockCache;
        this.cache = new LRUCache<>("key-cache", "embedded.storage.key.cache.capacity", "32M",
                128, new EstimateSizeValueCalculator<>(), new EstimateSizeValueCalculator<>());
    }

    private SlotKeyReadWrite get(short slot) {
        return CamelliaMapUtils.computeIfAbsent(map, slot, s -> new SlotKeyReadWrite(slot, executor, blockCache));
    }

    public KeyInfo get(short slot, CacheKey key) throws IOException {
        KeyInfo keyInfo = cache.get(key);
        if (keyInfo != null) {
            if (keyInfo == KeyInfo.DELETE) {
                return null;
            }
            return keyInfo;
        }
        keyInfo = get(slot).get(key);
        if (keyInfo == null) {
            cache.put(key, KeyInfo.DELETE);
            return null;
        }
        if (keyInfo.isExpire()) {
            cache.put(key, KeyInfo.DELETE);
            return null;
        }
        cache.put(key, keyInfo);
        return keyInfo;
    }

    public KeyInfo getForCompact(short slot, CacheKey key) throws IOException {
        KeyInfo keyInfo = cache.get(new CacheKey(key.key()));
        if (keyInfo != null) {
            if (keyInfo == KeyInfo.DELETE) {
                return null;
            }
            return keyInfo;
        }
        keyInfo = get(slot).get(key);
        if (keyInfo == null) {
            return null;
        }
        if (keyInfo.isExpire()) {
            return null;
        }
        return keyInfo;
    }

    public void put(short slot, KeyInfo keyInfo) {
        cache.put(new CacheKey(keyInfo.getKey()), keyInfo);
        get(slot).put(keyInfo);
    }

    public void delete(short slot, CacheKey key) {
        cache.put(key, KeyInfo.DELETE);
        get(slot).delete(key);
    }

    public void flushPrepare(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return;
        }
        slotKeyReadWrite.flushPrepare();
    }

    public CompletableFuture<FlushResult> flush(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        return slotKeyReadWrite.flush();
    }

    public boolean needFlush(short slot) {
        SlotKeyReadWrite slotKeyReadWrite = map.get(slot);
        if (slotKeyReadWrite == null) {
            return false;
        }
        return slotKeyReadWrite.needFlush();
    }
}

package com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ValueWrapper;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.EstimateSizeValueCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.LRUCache;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.cache.SizeCalculator;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.enums.FlushResult;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.ValueFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block.StringBlockReadWrite;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by caojiajun on 2025/1/3
 */
public class StringReadWrite {

    private static final String READ_CACHE_CONFIG_KEY = "local.storage.string.read.cache.capacity";
    private static final String WRITE_CACHE_CONFIG_KEY = "local.storage.string.write.cache.capacity";

    private final ConcurrentHashMap<Short, SlotStringReadWrite> map = new ConcurrentHashMap<>();

    private final ValueFlushExecutor flushExecutor;
    private final StringBlockReadWrite stringBlockReadWrite;

    private final LRUCache<Key, byte[]> readCache;
    private final LRUCache<Key, byte[]> writeCache;

    public StringReadWrite(ValueFlushExecutor flushExecutor, StringBlockReadWrite stringBlockReadWrite) {
        this.flushExecutor = flushExecutor;
        this.stringBlockReadWrite = stringBlockReadWrite;
        this.readCache = new LRUCache<>("string-read-cache", READ_CACHE_CONFIG_KEY, "32M", 1024, new EstimateSizeValueCalculator<>(), SizeCalculator.BYTES_INSTANCE);
        this.writeCache = new LRUCache<>("string-write-cache", WRITE_CACHE_CONFIG_KEY, "32M", 1024, new EstimateSizeValueCalculator<>(), SizeCalculator.BYTES_INSTANCE);
    }

    public void put(short slot, KeyInfo keyInfo, byte[] data) throws IOException {
        Key key = new Key(keyInfo.getKey());
        byte[] bytes = readCache.get(key);
        if (bytes != null) {
            readCache.put(key, data);
        } else {
            writeCache.put(key, data);
        }
        get(slot).put(keyInfo, data);
    }

    public byte[] get(short slot, KeyInfo keyInfo) throws IOException {
        Key key = new Key(keyInfo.getKey());
        byte[] bytes = readCache.get(key);
        if (bytes != null) {
            return bytes;
        }
        bytes = writeCache.get(key);
        if (bytes != null) {
            readCache.put(key, bytes);
            writeCache.delete(key);
            return bytes;
        }
        bytes = get(slot).get(keyInfo);
        readCache.put(key, bytes);
        return bytes;
    }

    public ValueWrapper<byte[]> getForRunToCompletion(short slot, KeyInfo keyInfo) {
        Key key = new Key(keyInfo.getKey());
        byte[] bytes1 = readCache.get(key);
        if (bytes1 != null) {
            return () -> bytes1;
        }
        byte[] bytes2 = writeCache.get(key);
        if (bytes2 != null) {
            readCache.put(key, bytes2);
            writeCache.delete(key);
            return () -> bytes2;
        }
        return get(slot).getForRunToCompletion(keyInfo);
    }

    public CompletableFuture<FlushResult> flush(short slot, Map<Key, KeyInfo> keyMap) throws IOException {
        SlotStringReadWrite slotStringReadWrite = get(slot);
        if (slotStringReadWrite == null) {
            return CompletableFuture.completedFuture(FlushResult.OK);
        }
        return slotStringReadWrite.flush(keyMap);
    }

    public boolean needFlush(short slot) {
        SlotStringReadWrite slotStringReadWrite = get(slot);
        if (slotStringReadWrite == null) {
            return false;
        }
        return slotStringReadWrite.needFlush();
    }

    private SlotStringReadWrite get(short slot) {
        return CamelliaMapUtils.computeIfAbsent(map, slot, s -> new SlotStringReadWrite(slot, flushExecutor, stringBlockReadWrite));
    }

}

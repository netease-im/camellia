package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.block.KeyBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.persist.KeyFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.IKeyManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot.KeyManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.IValueManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.ValueManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.persist.ValueFlushExecutor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.StringReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.block.StringBlockReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.IWalManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.WalManifest;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.WalReadWrite;

import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/10
 */
public class LocalStorageReadWrite {

    //
    private WalReadWrite walReadWrite;
    private IWalManifest walManifest;
    //
    private final IKeyManifest keyManifest;
    private final IValueManifest valueManifest;
    //
    private final KeyBlockReadWrite keyBlockReadWrite;
    private final StringBlockReadWrite stringBlockReadWrite;
    //
    private final KeyFlushExecutor keyFlushExecutor;
    private final ValueFlushExecutor valueFlushExecutor;
    //
    private final KeyReadWrite keyReadWrite;
    private final StringReadWrite stringReadWrite;

    public LocalStorageReadWrite(String dir) throws IOException {
        FlushExecutor flushExecutor = LocalStorageExecutors.getInstance().getFlushExecutor();

        keyManifest = new KeyManifest(dir);
        keyManifest.load();
        valueManifest = new ValueManifest(dir);
        valueManifest.load();

        keyBlockReadWrite = new KeyBlockReadWrite(keyManifest);
        stringBlockReadWrite = new StringBlockReadWrite(valueManifest);

        keyFlushExecutor = new KeyFlushExecutor(flushExecutor, keyManifest, keyBlockReadWrite);
        valueFlushExecutor = new ValueFlushExecutor(flushExecutor, valueManifest, stringBlockReadWrite);

        keyReadWrite = new KeyReadWrite(keyFlushExecutor, keyBlockReadWrite);
        stringReadWrite = new StringReadWrite(valueFlushExecutor, stringBlockReadWrite);
    }

    public WalReadWrite getWalReadWrite() {
        return walReadWrite;
    }

    public IWalManifest getWalManifest() {
        return walManifest;
    }

    public IKeyManifest getKeyManifest() {
        return keyManifest;
    }

    public IValueManifest getValueManifest() {
        return valueManifest;
    }

    public KeyBlockReadWrite getKeyBlockReadWrite() {
        return keyBlockReadWrite;
    }

    public StringBlockReadWrite getStringBlockReadWrite() {
        return stringBlockReadWrite;
    }

    public KeyFlushExecutor getKeyFlushExecutor() {
        return keyFlushExecutor;
    }

    public ValueFlushExecutor getValueFlushExecutor() {
        return valueFlushExecutor;
    }

    public KeyReadWrite getKeyReadWrite() {
        return keyReadWrite;
    }

    public StringReadWrite getStringReadWrite() {
        return stringReadWrite;
    }
}

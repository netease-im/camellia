package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.command.LocalStorageReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyReadWrite;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.string.StringReadWrite;

/**
 * Created by caojiajun on 2025/1/16
 */
public record StringWalEntry(KeyInfo keyInfo, byte[] value) implements WalEntry {

    @Override
    public void recover(short slot, LocalStorageReadWrite readWrite) throws Exception {
        KeyReadWrite keyReadWrite = readWrite.getKeyReadWrite();
        StringReadWrite stringReadWrite = readWrite.getStringReadWrite();
        keyReadWrite.put(slot, keyInfo);
        if (value != null) {
            stringReadWrite.put(slot, keyInfo, value);
        }
    }
}

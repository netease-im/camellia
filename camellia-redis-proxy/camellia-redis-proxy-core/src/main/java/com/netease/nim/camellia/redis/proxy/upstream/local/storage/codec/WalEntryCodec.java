package com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.StringWalEntry;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.WalEntry;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal.WalEntryType;

/**
 * Created by caojiajun on 2025/1/17
 */
public class WalEntryCodec {

    public static byte[] encode(WalEntry walEntry) {
        if (walEntry instanceof StringWalEntry) {
            Pack pack = new Pack();
            pack.putByte(WalEntryType.string.getType());
            pack.putMarshallable(((StringWalEntry) walEntry).keyInfo());
            byte[] value = ((StringWalEntry) walEntry).value();
            if (value != null) {
                pack.putVarbin(value);
            }
            pack.getBuffer().capacity(pack.getBuffer().readableBytes());
            return pack.getBuffer().array();
        } else {
            throw new IllegalArgumentException("not support WalEntry");
        }
    }

    public static WalEntry decode(byte[] data) {
        Unpack unpack = new Unpack(data);
        byte type = unpack.popByte();
        if (type == WalEntryType.string.getType()) {
            KeyInfo keyInfo = new KeyInfo();
            unpack.popMarshallable(keyInfo);
            byte[] value = null;
            if (unpack.getBuffer().readableBytes() > 0) {
                value = unpack.popVarbin();
            }
            return new StringWalEntry(keyInfo, value);
        } else {
            throw new IllegalArgumentException("not support WalEntry");
        }
    }
}

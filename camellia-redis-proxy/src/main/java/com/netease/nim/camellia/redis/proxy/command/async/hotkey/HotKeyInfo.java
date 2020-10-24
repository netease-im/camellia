package com.netease.nim.camellia.redis.proxy.command.async.hotkey;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class HotKeyInfo {
    private final byte[] key;
    private final long count;

    public HotKeyInfo(byte[] key, long count) {
        this.key = key;
        this.count = count;
    }

    public byte[] getKey() {
        return key;
    }

    public long getCount() {
        return count;
    }
}

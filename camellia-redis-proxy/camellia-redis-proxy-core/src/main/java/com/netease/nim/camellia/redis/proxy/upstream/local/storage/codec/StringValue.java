package com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2025/1/9
 */
public record StringValue(byte[] key, byte[] value) {

    public static byte[] encode(byte[] key, byte[] value) {
        Pack pack = new Pack(key.length + value.length + 8);
        pack.putVarbin(key);
        pack.putVarbin(value);
        pack.getBuffer().capacity(pack.getBuffer().readableBytes());
        return pack.getBuffer().array();
    }

    public static StringValue decode(byte[] data) {
        Unpack unpack = new Unpack(data);
        byte[] key = unpack.popVarbin();
        byte[] value = unpack.popVarbin();
        return new StringValue(key, value);
    }

    public static Map<Key, byte[]> encodeMap(Map<KeyInfo, byte[]> map) {
        Map<Key, byte[]> result = new HashMap<>();
        for (Map.Entry<KeyInfo, byte[]> entry : map.entrySet()) {
            result.put(new Key(entry.getKey().getKey()), StringValue.encode(entry.getKey().getKey(), entry.getValue()));
        }
        return result;
    }
}

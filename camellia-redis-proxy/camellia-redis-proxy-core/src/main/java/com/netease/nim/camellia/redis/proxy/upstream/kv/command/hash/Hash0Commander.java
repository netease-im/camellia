package com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisHash;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/5/15
 */
public abstract class Hash0Commander extends Commander {

    public Hash0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final RedisHash loadLRUCache(int slot, KeyMeta keyMeta, byte[] key) {
        Map<BytesKey, byte[]> map = hgetallFromKv(slot, keyMeta, key);
        return new RedisHash(map);
    }

    protected final Map<BytesKey, byte[]> hgetallFromKv(int slot, KeyMeta keyMeta, byte[] key) {
        Map<BytesKey, byte[]> map = new HashMap<>();
        byte[] startKey = keyDesign.hashFieldSubKey(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int hashMaxSize = kvConfig.hashMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return map;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] field = keyDesign.decodeHashFieldBySubKey(keyValue.getKey(), key);
                map.put(new BytesKey(field), keyValue.getValue());
            }
            if (scan.size() < limit) {
                return map;
            }
            //
            if (map.size() >= hashMaxSize) {
                ErrorLogCollector.collect(Hash0Commander.class, "redis.hash.size exceed " + hashMaxSize + ", key = " + Utils.bytesToString(key));
                return map;
            }
        }
    }
}

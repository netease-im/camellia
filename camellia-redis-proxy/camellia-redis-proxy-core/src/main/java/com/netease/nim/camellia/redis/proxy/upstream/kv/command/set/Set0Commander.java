package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/8/5
 */
public abstract class Set0Commander extends Commander {

    public Set0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final RedisSet loadLRUCache(KeyMeta keyMeta, byte[] key) {
        Set<BytesKey> set = smembersFromKv(keyMeta, key);
        return new RedisSet(set);
    }

    protected final Set<BytesKey> smembersFromKv(KeyMeta keyMeta, byte[] key) {
        Set<BytesKey> set = new HashSet<>();
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int setMaxSize = kvConfig.setMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : scan) {
                byte[] member = keyDesign.decodeSetMemberBySubKey(keyValue.getKey(), key);
                set.add(new BytesKey(member));
                startKey = keyValue.getKey();
                if (set.size() >= setMaxSize) {
                    break;
                }
            }
            if (scan.size() < limit) {
                break;
            }
        }
        return set;
    }
}

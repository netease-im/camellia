package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/6/3
 */
public abstract class ZSet0Commander extends Commander {

    public ZSet0Commander(CommanderConfig commanderConfig) {
        super(commanderConfig);
    }

    protected final ZSet loadLRUCache(KeyMeta keyMeta, byte[] key) {
        EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
        if (encodeVersion == EncodeVersion.version_0 || encodeVersion == EncodeVersion.version_1 || encodeVersion == EncodeVersion.version_2) {
            List<ZSetTuple> list = zrangeAllFromKv(keyMeta, key);
            Map<BytesKey, Double> memberMap = new HashMap<>(list.size());
            for (ZSetTuple tuple : list) {
                memberMap.put(tuple.getMember(), tuple.getScore());
            }
            return new ZSet(memberMap);
        }
        return null;
    }

    protected final List<ZSetTuple> zrangeAllFromKv(KeyMeta keyMeta, byte[] key) {
        List<ZSetTuple> list = new ArrayList<>();
        byte[] startKey = keyDesign.zsetMemberSubKey1(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int zsetMaxSize = kvConfig.zsetMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : scan) {
                byte[] member = keyDesign.decodeZSetMemberBySubKey1(keyValue.getKey(), key);
                list.add(new ZSetTuple(new BytesKey(member), Utils.bytesToDouble(keyValue.getValue())));
                startKey = keyValue.getKey();
                if (list.size() >= zsetMaxSize) {
                    break;
                }
            }
            if (scan.size() < limit) {
                break;
            }
        }
        return list;
    }
}

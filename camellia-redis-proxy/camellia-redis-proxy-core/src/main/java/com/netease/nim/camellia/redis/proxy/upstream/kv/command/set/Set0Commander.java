package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

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

    protected final Set<BytesKey> srandmemberFromKv(KeyMeta keyMeta, byte[] key, int count) {
        Set<BytesKey> set = new HashSet<>();
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(startKey, prefix, count, Sort.ASC, false);
            if (scan.isEmpty()) {
                break;
            }
            for (KeyValue keyValue : scan) {
                byte[] member = keyDesign.decodeSetMemberBySubKey(keyValue.getKey(), key);
                set.add(new BytesKey(member));
                startKey = keyValue.getKey();
                if (set.size() >= count) {
                    break;
                }
            }
            if (scan.size() < count) {
                break;
            }
        }
        return set;
    }

    protected final void writeMembers(KeyMeta keyMeta, byte[] key, byte[] cacheKey, int memberSize, Set<BytesKey> memberSet, Result result) {
        List<KeyValue> list = new ArrayList<>(memberSize);
        for (BytesKey bytesKey : memberSet) {
            byte[] member = bytesKey.getKey();
            byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
            KeyValue keyValue = new KeyValue(subKey, new byte[0]);
            list.add(keyValue);
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(cacheKey, result, () -> kvClient.batchPut(list));
        } else {
            kvClient.batchPut(list);
        }
    }

    protected final void removeMembers(KeyMeta keyMeta, byte[] key, byte[] cacheKey, Collection<BytesKey> members, Result result) {
        byte[][] subKeys = new byte[members.size()][];
        int i = 0;
        for (BytesKey bytesKey : members) {
            byte[] member = bytesKey.getKey();
            byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
            subKeys[i] = subKey;
            i++;
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(cacheKey, result, () -> {
                kvClient.batchDelete(subKeys);
            });
        } else {
            kvClient.batchDelete(subKeys);
        }
    }

    protected final Map<BytesKey, Boolean> smismemberFromKv(KeyMeta keyMeta, byte[] key, Collection<BytesKey> members) {
        byte[][] subKeys = new byte[members.size()][];
        int i = 0;
        for (BytesKey member : members) {
            subKeys[i] = keyDesign.setMemberSubKey(keyMeta, key, member.getKey());
            i ++;
        }
        List<KeyValue> keyValues = kvClient.batchGet(subKeys);
        Map<BytesKey, Boolean> map = new HashMap<>();
        for (KeyValue keyValue : keyValues) {
            if (keyValue == null || keyValue.getKey() == null) {
                continue;
            }
            byte[] member = keyDesign.decodeSetMemberBySubKey(keyValue.getKey(), key);
            map.put(new BytesKey(member), true);
        }
        return map;
    }

    protected final void updateKeyMeta(KeyMeta keyMeta, byte[] key, int add) {
        if (add > 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) + add;
            byte[] extra = BytesUtils.toBytes(count);
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
            keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
        } else if (add < 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) + add;
            if (count > 0) {
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                keyMetaServer.createOrUpdateKeyMeta(key, keyMeta);
            } else {
                keyMetaServer.deleteKeyMeta(key);
            }
        }
    }

    protected final byte[] smembersCacheMillis() {
        return Utils.stringToBytes(String.valueOf(cacheConfig.smembersCacheMillis()));
    }
}

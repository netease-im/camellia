package com.netease.nim.camellia.redis.proxy.upstream.kv.command.set;

import com.netease.nim.camellia.redis.proxy.monitor.KvLoadCacheMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.kv.buffer.Result;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.hash.Hash0Commander;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
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

    protected final RedisSet loadLRUCache(int slot, KeyMeta keyMeta, byte[] key) {
        KvLoadCacheMonitor.update(kvConfig.getNamespace(), redisCommand().strRaw());
        Set<BytesKey> set = smembersFromKv(slot, keyMeta, key);
        return new RedisSet(set);
    }

    protected final Set<BytesKey> smembersFromKv(int slot, KeyMeta keyMeta, byte[] key) {
        Set<BytesKey> set = new HashSet<>();
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit = kvConfig.scanBatch();
        int setMaxSize = kvConfig.setMaxSize();
        while (true) {
            List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return set;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] member = keyDesign.decodeSetMemberBySubKey(keyValue.getKey(), key);
                set.add(new BytesKey(member));
            }
            if (scan.size() < limit) {
                return set;
            }
            //
            if (set.size() >= setMaxSize) {
                ErrorLogCollector.collect(Hash0Commander.class, "redis.set.size exceed " + setMaxSize + ", key = " + Utils.bytesToString(key));
                return set;
            }
        }
    }

    protected final Set<BytesKey> srandmemberFromKv(int slot, KeyMeta keyMeta, byte[] key, int count) {
        Set<BytesKey> result = new HashSet<>();
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        byte[] prefix = startKey;
        int limit;
        while (true) {
            limit = Math.min(kvConfig.scanBatch(), count - result.size());
            List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
            if (scan.isEmpty()) {
                return result;
            }
            for (KeyValue keyValue : scan) {
                if (keyValue == null || keyValue.getKey() == null) {
                    continue;
                }
                startKey = keyValue.getKey();
                byte[] member = keyDesign.decodeSetMemberBySubKey(keyValue.getKey(), key);
                result.add(new BytesKey(member));
                if (result.size() >= count) {
                    return result;
                }
            }
            if (scan.size() < limit) {
                return result;
            }
        }
    }

    protected final void writeMembers(int slot, KeyMeta keyMeta, byte[] key, Set<BytesKey> memberSet, Result result) {
        List<KeyValue> list = new ArrayList<>(memberSet.size());
        for (BytesKey bytesKey : memberSet) {
            byte[] member = bytesKey.getKey();
            byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
            KeyValue keyValue = new KeyValue(subKey, new byte[0]);
            list.add(keyValue);
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(slot, result, () -> kvClient.batchPut(slot, list));
        } else {
            kvClient.batchPut(slot, list);
        }
    }

    protected final void removeMembers(int slot, KeyMeta keyMeta, byte[] key, Collection<BytesKey> members, Result result, boolean checkSCard) {
        byte[][] subKeys = new byte[members.size()][];
        int i = 0;
        for (BytesKey bytesKey : members) {
            byte[] member = bytesKey.getKey();
            byte[] subKey = keyDesign.setMemberSubKey(keyMeta, key, member);
            subKeys[i] = subKey;
            i++;
        }
        if (result.isKvWriteDelayEnable()) {
            submitAsyncWriteTask(slot, result, () -> {
                kvClient.batchDelete(slot, subKeys);
                if (checkSCard) {
                    if (getSizeFromKv(slot, keyMeta, key) == 0) {
                        keyMetaServer.deleteKeyMeta(slot, key);
                    }
                }
            });
        } else {
            kvClient.batchDelete(slot, subKeys);
            if (checkSCard) {
                if (getSizeFromKv(slot, keyMeta, key) == 0) {
                    keyMetaServer.deleteKeyMeta(slot, key);
                }
            }
        }
    }

    private long getSizeFromKv(int slot, KeyMeta keyMeta, byte[] key) {
        byte[] startKey = keyDesign.setMemberSubKey(keyMeta, key, new byte[0]);
        return kvClient.countByPrefix(slot, startKey, startKey, false);
    }

    protected final Map<BytesKey, Boolean> smismemberFromKv(int slot, KeyMeta keyMeta, byte[] key, Collection<BytesKey> members) {
        byte[][] subKeys = new byte[members.size()][];
        int i = 0;
        for (BytesKey member : members) {
            subKeys[i] = keyDesign.setMemberSubKey(keyMeta, key, member.getKey());
            i ++;
        }
        List<KeyValue> keyValues = kvClient.batchGet(slot, subKeys);
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

    protected final void updateKeyMeta(int slot, KeyMeta keyMeta, byte[] key, int add) {
        if (add > 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) + add;
            byte[] extra = BytesUtils.toBytes(count);
            keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
            keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
        } else if (add < 0) {
            int count = BytesUtils.toInt(keyMeta.getExtra()) + add;
            if (count > 0) {
                byte[] extra = BytesUtils.toBytes(count);
                keyMeta = new KeyMeta(keyMeta.getEncodeVersion(), keyMeta.getKeyType(), keyMeta.getKeyVersion(), keyMeta.getExpireTime(), extra);
                keyMetaServer.createOrUpdateKeyMeta(slot, key, keyMeta);
            } else {
                keyMetaServer.deleteKeyMeta(slot, key);
            }
        }
    }
}
